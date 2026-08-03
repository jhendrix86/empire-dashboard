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

    /** Raw EMPIRE_MAX_RUN_COST_USD value, kept distinct from [maxRunCostUsd] so a typo
     *  (e.g. "$2.00") can be told apart from the variable being unset -- both would
     *  otherwise collapse to null and silently mean "unlimited". */
    val maxRunCostUsdRaw: String? by lazy { env("EMPIRE_MAX_RUN_COST_USD")?.trim()?.takeIf { it.isNotEmpty() } }

    /** Optional ceiling on estimated LLM spend per pipeline run; unset means unlimited. */
    val maxRunCostUsd: Double? by lazy { maxRunCostUsdRaw?.toDoubleOrNull()?.takeIf { it > 0 } }

    /** False only when EMPIRE_MAX_RUN_COST_USD is set to something that isn't a positive number. */
    val maxRunCostUsdIsValid: Boolean by lazy { maxRunCostUsdRaw == null || maxRunCostUsd != null }

    /** Opt-in: expose the server on the LAN (e.g. for the Android app) instead of loopback-only. */
    val bindAllInterfaces: Boolean by lazy { env("EMPIRE_BIND_ALL")?.toBooleanStrictOrNull() ?: false }

    /** Required whenever [bindAllInterfaces] is true; checked on every mutating route. */
    val authToken: String? by lazy { env("EMPIRE_AUTH_TOKEN") }

    /** Optional outbound webhook (Slack/Discord/ntfy/etc.) notified on a sale or a failed run. */
    val notifyWebhookUrl: String? by lazy { env("EMPIRE_NOTIFY_WEBHOOK_URL")?.trim()?.takeIf { it.isNotEmpty() } }

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
