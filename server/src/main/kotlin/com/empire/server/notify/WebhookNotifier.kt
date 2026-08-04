package com.empire.server.notify

import com.empire.server.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

@Serializable
private data class WebhookPayload(val event: String, val text: String)

/**
 * POSTs a small `{event, text}` JSON payload to EMPIRE_NOTIFY_WEBHOOK_URL, if set -- point
 * it at a Slack/Discord incoming webhook, ntfy.sh, or any endpoint that accepts JSON,
 * without this app needing to know which. A no-op when unset, and failures are logged
 * rather than propagated: a notification going missing must never take down the pipeline
 * run or the revenue write that triggered it.
 */
class WebhookNotifier(private val webhookUrl: String? = AppConfig.notifyWebhookUrl) : Notifier {
    private val logger = LoggerFactory.getLogger(WebhookNotifier::class.java)
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    override suspend fun notify(event: String, message: String) {
        val url = webhookUrl ?: return
        runCatching {
            client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(WebhookPayload(event = event, text = message))
            }
        }.onFailure { e -> logger.warn("notification webhook failed for event '{}': {}", event, e.message) }
    }
}
