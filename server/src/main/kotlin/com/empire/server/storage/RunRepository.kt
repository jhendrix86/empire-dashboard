package com.empire.server.storage

import com.empire.server.config.AppConfig
import com.empire.server.orchestration.RunManifest
import java.io.File

class RunRepository(dataDir: File = AppConfig.dataDir) {
    private val runsDir = File(dataDir, "runs").apply { mkdirs() }
    private val currentRunFile = File(dataDir, "current_run_id.txt")

    fun runDir(runId: String): File = File(runsDir, runId).apply { mkdirs() }
    private fun manifestFile(runId: String) = File(runDir(runId), "manifest.json")
    private fun logsFile(runId: String) = File(runDir(runId), "logs.jsonl")

    fun currentRunId(): String? =
        currentRunFile.takeIf { it.exists() }?.readText()?.trim()?.takeIf { it.isNotBlank() }

    fun setCurrentRunId(runId: String) {
        writeTextAtomically(currentRunFile, runId)
    }

    fun save(manifest: RunManifest) {
        writeTextAtomically(manifestFile(manifest.runId), appJson.encodeToString(RunManifest.serializer(), manifest))
    }

    fun load(runId: String): RunManifest? {
        val file = manifestFile(runId)
        if (!file.exists()) return null
        return try {
            appJson.decodeFromString(RunManifest.serializer(), file.readText())
        } catch (e: Exception) {
            null
        }
    }

    fun currentManifest(): RunManifest? = currentRunId()?.let(::load)

    fun allRunIds(): List<String> =
        runsDir.listFiles { f -> f.isDirectory }
            ?.filter { manifestFile(it.name).exists() }
            ?.map { it.name }
            ?.sortedDescending()
            ?: emptyList()

    fun recentRuns(limit: Int = 20): List<RunManifest> =
        allRunIds().take(limit).mapNotNull(::load)

    fun appendLog(runId: String, line: String) {
        logsFile(runId).parentFile?.mkdirs()
        logsFile(runId).appendText(line + "\n")
    }

    fun newLogLinesSince(runId: String, cursor: Int): Pair<List<String>, Int> {
        val file = logsFile(runId)
        if (!file.exists()) return emptyList<String>() to cursor
        val lines = file.readLines()
        val fromIndex = cursor.coerceIn(0, lines.size)
        return lines.subList(fromIndex, lines.size) to lines.size
    }
}
