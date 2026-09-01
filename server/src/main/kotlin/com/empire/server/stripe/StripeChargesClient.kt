package com.empire.server.stripe

import com.empire.server.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import java.util.Base64
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

private val stripeJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

@Serializable
private data class ChargeListResponse(val data: List<ChargeDto> = emptyList())

@Serializable
private data class ChargeDto(
    val id: String,
    val amount: Long = 0,
    val paid: Boolean = false,
    val refunded: Boolean = false,
    val status: String = "",
    val created: Long = 0,
    @SerialName("receipt_email") val receiptEmail: String? = null,
    val description: String? = null,
    @SerialName("billing_details") val billingDetails: BillingDetailsDto? = null
) {
    fun toStripeCharge() = StripeCharge(
        id = id,
        amountUsd = amount / 100.0, // Stripe amounts are in cents
        email = receiptEmail ?: billingDetails?.email,
        description = description,
        createdUnixSeconds = created,
        successful = paid && !refunded && status == "succeeded"
    )
}

@Serializable
private data class BillingDetailsDto(val email: String? = null)

/** Thin wrapper around Stripe's Charges API. A no-op (never throws) when STRIPE_SECRET_KEY
 *  is unset or the request fails, since a sync hiccup must never break the caller. */
class StripeChargesClient(private val secretKey: String? = AppConfig.stripeSecretKey) : StripeClient {
    private val logger = LoggerFactory.getLogger(StripeChargesClient::class.java)
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json(stripeJson) }
    }

    override suspend fun listRecentCharges(): List<StripeCharge> {
        val key = secretKey ?: return emptyList()
        return runCatching {
            client.get("https://api.stripe.com/v1/charges") {
                header("Authorization", "Basic " + Base64.getEncoder().encodeToString("$key:".toByteArray()))
                parameter("limit", 100)
            }.body<ChargeListResponse>().data.map { it.toStripeCharge() }
        }.getOrElse { e ->
            logger.warn("Stripe charge sync failed: {}", e.message)
            emptyList()
        }
    }
}
