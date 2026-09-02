package com.empire.server.etsy

import com.empire.server.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import java.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

private val etsyJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

@Serializable
private data class TokenResponse(
    val access_token: String,
    val refresh_token: String,
    val expires_in: Long = 3600
)

@Serializable
private data class CreateListingRequest(
    val quantity: Int,
    val title: String,
    val description: String,
    val price: Double,
    val who_made: String = "i_did",
    val when_made: String = "made_to_order",
    val taxonomy_id: Long,
    val is_digital: Boolean = true
)

@Serializable
private data class CreateListingResponse(val listing_id: Long = 0)

/** Thin wrapper around Etsy's Open API v3. A no-op (never throws) whenever any of
 *  ETSY_API_KEY/ETSY_SHOP_ID/ETSY_TAXONOMY_ID is unset, no refresh token is available yet,
 *  or a request fails -- a listing hiccup must never break the run. Always creates the
 *  listing via Etsy's createDraftListing endpoint, which is draft-only by design: nothing
 *  publishes without the operator reviewing it first.
 *
 *  Etsy access tokens expire after ~1 hour and every refresh rotates the refresh token
 *  itself, so this client re-authenticates on demand via [EtsyTokenStore] rather than
 *  holding a single long-lived credential like the Stripe/Shopify clients do. */
class EtsyApiClient(
    private val apiKey: String? = AppConfig.etsyApiKey,
    private val shopId: String? = AppConfig.etsyShopId,
    private val taxonomyId: Long? = AppConfig.etsyTaxonomyId,
    private val tokenStore: EtsyTokenStore = EtsyTokenStore()
) : EtsyClient {
    private val logger = LoggerFactory.getLogger(EtsyApiClient::class.java)
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json(etsyJson) }
    }

    override suspend fun createDraftListing(title: String, description: String, priceUsd: Double?): EtsyListing? {
        val key = apiKey ?: return null
        val shop = shopId ?: return null
        val taxonomy = taxonomyId ?: return null

        return runCatching {
            val accessToken = ensureAccessToken(key) ?: return null
            val listing = client.post("https://api.etsy.com/v3/application/shops/$shop/listings") {
                header("x-api-key", key)
                header("Authorization", "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(
                    CreateListingRequest(
                        quantity = 999,
                        title = title,
                        description = description,
                        price = priceUsd ?: 0.0,
                        taxonomy_id = taxonomy
                    )
                )
            }.body<CreateListingResponse>()

            EtsyListing(listingId = listing.listing_id, url = "https://www.etsy.com/listing/${listing.listing_id}")
        }.getOrElse { e ->
            logger.warn("Etsy draft listing creation failed: {}", e.message)
            null
        }
    }

    /** Returns a valid access token, refreshing (and persisting the rotated refresh token)
     *  if the cached one is missing or close to expiry. Null means Etsy hasn't been
     *  authorized yet (no refresh token available from either the token store or
     *  ETSY_REFRESH_TOKEN). */
    private suspend fun ensureAccessToken(key: String): String? {
        val tokens = tokenStore.read()
        val now = Instant.now().epochSecond
        if (tokens.accessToken.isNotEmpty() && tokens.expiresAtEpochSeconds > now + 60) {
            return tokens.accessToken
        }
        if (tokens.refreshToken.isEmpty()) return null

        val response = client.submitForm(
            url = "https://api.etsy.com/v3/public/oauth/token",
            formParameters = Parameters.build {
                append("grant_type", "refresh_token")
                append("client_id", key)
                append("refresh_token", tokens.refreshToken)
            }
        ).body<TokenResponse>()

        tokenStore.write(
            EtsyTokens(
                accessToken = response.access_token,
                refreshToken = response.refresh_token,
                expiresAtEpochSeconds = now + response.expires_in
            )
        )
        return response.access_token
    }
}
