package com.empire.server.config

import java.io.File

object AppConfig {
    val dataDir: File = File(System.getenv("EMPIRE_DATA_DIR") ?: "server/data").apply { mkdirs() }

    /** "anthropic" (default) or "openai" -- picks which LlmClient Application.kt constructs. */
    val llmProvider: String by lazy { env("LLM_PROVIDER")?.lowercase() ?: "anthropic" }

    val anthropicApiKey: String? by lazy { env("ANTHROPIC_API_KEY") }
    val anthropicModel: String by lazy { env("ANTHROPIC_MODEL") ?: "claude-sonnet-5" }

    val openAiApiKey: String? by lazy { env("OPENAI_API_KEY") }
    val openAiModel: String by lazy { env("OPENAI_MODEL") ?: "gpt-4o-mini" }

    /** Opt-in: expose the server on the LAN (e.g. for the Android app) instead of loopback-only. */
    val bindAllInterfaces: Boolean by lazy { env("EMPIRE_BIND_ALL")?.toBooleanStrictOrNull() ?: false }

    /** Required whenever [bindAllInterfaces] is true; checked on every mutating route. */
    val authToken: String? by lazy { env("EMPIRE_AUTH_TOKEN") }

    fun env(key: String): String? = System.getenv(key) ?: dotEnv[key]

    private val dotEnv: Map<String, String> by lazy {
        listOf(File(".env"), File("server/.env"))
            .firstOrNull { it.exists() }
            ?.readLines()
            ?.mapNotNull(::parseLine)
            ?.toMap()
            ?: emptyMap()
    }

    private fun parseLine(line: String): Pair<String, String>? {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) return null
        val idx = trimmed.indexOf('=')
        if (idx < 0) return null
        return trimmed.substring(0, idx).trim() to trimmed.substring(idx + 1).trim()
    }
}
