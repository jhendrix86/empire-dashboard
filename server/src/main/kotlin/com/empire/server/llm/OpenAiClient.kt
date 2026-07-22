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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val openAiJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

@Serializable
private data class ChatMessage(val role: String, val content: String)

@Serializable
private data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>
)

@Serializable
private data class ChatChoice(val message: ChatMessage)

@Serializable
private data class ChatErrorBody(val message: String = "", val type: String = "")

@Serializable
private data class ChatResponse(
    val choices: List<ChatChoice> = emptyList(),
    val error: ChatErrorBody? = null
)

/** Thin wrapper around OpenAI's Chat Completions API -- alternative to AnthropicClient. */
class OpenAiClient(
    private val apiKey: String? = AppConfig.openAiApiKey,
    private val model: String = AppConfig.openAiModel
) : LlmClient {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json(openAiJson) }
    }

    override suspend fun complete(systemPrompt: String, userPrompt: String): String {
        val key = apiKey ?: error("OPENAI_API_KEY is not set")

        val response = client.post("https://api.openai.com/v1/chat/completions") {
            header("Authorization", "Bearer $key")
            contentType(ContentType.Application.Json)
            setBody(
                ChatRequest(
                    model = model,
                    messages = listOf(
                        ChatMessage(role = "system", content = systemPrompt),
                        ChatMessage(role = "user", content = userPrompt)
                    )
                )
            )
        }.body<ChatResponse>()

        response.error?.let { error("OpenAI API error (${it.type}): ${it.message}") }

        return response.choices.firstOrNull()?.message?.content?.trim().orEmpty()
    }
}
