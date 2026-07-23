package com.empire.server.llm

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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val anthropicJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

@Serializable
private data class AnthropicMessage(val role: String, val content: String)

@Serializable
private data class MessageRequest(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val system: String,
    val messages: List<AnthropicMessage>
)

@Serializable
private data class ContentBlock(val type: String = "", val text: String = "")

@Serializable
private data class ErrorBody(val type: String = "", val message: String = "")

@Serializable
private data class MessageResponse(
    val content: List<ContentBlock> = emptyList(),
    val error: ErrorBody? = null
)

/** Thin wrapper around the Anthropic Messages API -- one call per department persona. */
class AnthropicClient(
    private val apiKey: String? = AppConfig.anthropicApiKey,
    private val model: String = AppConfig.anthropicModel,
    private val maxTokens: Int = 4096
) : LlmClient {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json(anthropicJson) }
    }

    override suspend fun complete(systemPrompt: String, userPrompt: String): String {
        val key = apiKey ?: error("ANTHROPIC_API_KEY is not set")

        val response = client.post("https://api.anthropic.com/v1/messages") {
            header("x-api-key", key)
            header("anthropic-version", "2023-06-01")
            contentType(ContentType.Application.Json)
            setBody(
                MessageRequest(
                    model = model,
                    maxTokens = maxTokens,
                    system = systemPrompt,
                    messages = listOf(AnthropicMessage(role = "user", content = userPrompt))
                )
            )
        }.body<MessageResponse>()

        response.error?.let { error("Anthropic API error (${it.type}): ${it.message}") }

        return response.content.joinToString("\n") { it.text }.trim()
    }
}

/** Strips ```json fences and surrounding prose an LLM sometimes adds despite JSON-only instructions. */
fun extractJsonObject(text: String): String {
    val fenced = Regex("```(?:json)?\\s*([\\s\\S]*?)```").find(text)
    val candidate = fenced?.groupValues?.get(1) ?: text
    val start = candidate.indexOf('{')
    val end = candidate.lastIndexOf('}')
    return if (start >= 0 && end > start) candidate.substring(start, end + 1) else candidate.trim()
}
