package com.empire.server.storage

import com.empire.server.config.AppConfig
import com.empire.server.orchestration.RunManifest
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class RunRepository(dataDir: File = AppConfig.dataDir) {
    private val runsDir = File(dataDir, "runs").apply { mkdirs() }
    private val currentRunFile = File(dataDir, "current_run_id.txt")

    // Guards manifest read-modify-write: getProgress() (Ktor request threads) and the
    // orchestrator's stage transitions (background coroutine) both mutate the same
    // manifest.json, so read-then-save without a shared lock loses whichever write lands
    // second (e.g. a finished run's "done" status clobbered back to "running").
    private val manifestLock = ReentrantLock()

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

    /** Atomic read-modify-write: always use this (not separate load()+save() calls) to mutate a run's manifest. */
    fun update(runId: String, transform: (RunManifest) -> RunManifest): RunManifest? = manifestLock.withLock {
        val current = load(runId) ?: return@withLock null
        val next = transform(current)
        save(next)
        next
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
