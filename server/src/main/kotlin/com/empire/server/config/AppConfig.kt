package com.empire.server.config

import java.io.File

object AppConfig {
    val dataDir: File = File(System.getenv("EMPIRE_DATA_DIR") ?: "server/data").apply { mkdirs() }

    val anthropicApiKey: String? by lazy { env("ANTHROPIC_API_KEY") }

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
