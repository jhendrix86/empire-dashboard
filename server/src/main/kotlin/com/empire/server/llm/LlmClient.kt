package com.empire.server.llm

/** Common interface so orchestration stages don't care which provider is configured. */
interface LlmClient {
    suspend fun complete(systemPrompt: String, userPrompt: String): String
}
