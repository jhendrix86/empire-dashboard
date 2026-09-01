package com.empire.server.shopify

import com.empire.server.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

private val shopifyJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

@Serializable
private data class CreateProductRequest(val product: ProductPayload)

@Serializable
private data class ProductPayload(
    val title: String,
    val body_html: String,
    val status: String = "draft",
    val variants: List<VariantPayload>
)

@Serializable
private data class VariantPayload(val price: String)

@Serializable
private data class CreateProductResponse(val product: ProductResponseDto)

@Serializable
private data class ProductResponseDto(val id: Long = 0)

/** Thin wrapper around Shopify's Admin REST API. A no-op (never throws) when
 *  SHOPIFY_STORE_DOMAIN/SHOPIFY_ACCESS_TOKEN are unset or the request fails, since a
 *  listing hiccup must never break the run. Always creates the product as a DRAFT --
 *  never active/live -- so nothing publishes without the operator reviewing it first. */
class ShopifyAdminClient(
    private val storeDomain: String? = AppConfig.shopifyStoreDomain,
    private val accessToken: String? = AppConfig.shopifyAccessToken,
    private val apiVersion: String = AppConfig.shopifyApiVersion
) : ShopifyClient {
    private val logger = LoggerFactory.getLogger(ShopifyAdminClient::class.java)
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json(shopifyJson) }
    }

    override suspend fun createDraftProduct(title: String, bodyHtml: String, priceUsd: Double?): ShopifyProduct? {
        val domain = storeDomain ?: return null
        val token = accessToken ?: return null
        return runCatching {
            val product = client.post("https://$domain/admin/api/$apiVersion/products.json") {
                header("X-Shopify-Access-Token", token)
                contentType(ContentType.Application.Json)
                setBody(
                    CreateProductRequest(
                        ProductPayload(
                            title = title,
                            body_html = bodyHtml,
                            variants = listOf(VariantPayload(price = "%.2f".format(priceUsd ?: 0.0)))
                        )
                    )
                )
            }.body<CreateProductResponse>().product

            ShopifyProduct(id = product.id, adminUrl = "https://$domain/admin/products/${product.id}")
        }.getOrElse { e ->
            logger.warn("Shopify draft product creation failed: {}", e.message)
            null
        }
    }
}
