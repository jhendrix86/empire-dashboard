package com.empire.server.orchestration

import com.empire.dashboard.data.RunProgress
import com.empire.dashboard.data.RunRequest
import com.empire.dashboard.data.RunStartResponse
import com.empire.server.storage.RunRepository
import com.empire.server.util.newRunId
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RunOrchestrator(private val runRepository: RunRepository) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val startLock = Mutex()

    suspend fun startRun(request: RunRequest): RunStartResponse = startLock.withLock {
        val current = runRepository.currentManifest()
        if (current != null && current.status == RunStatus.RUNNING) {
            return@withLock RunStartResponse(started = false, error = "a run is already in progress")
        }

        val runId = newRunId()
        val manifest = RunManifest(runId = runId, createdAt = Instant.now().toString())
        runRepository.save(manifest)
        runRepository.setCurrentRunId(runId)
        log(runId, "[info] run $runId started")

        scope.launch { executeStages(runId, Stage.RESEARCH) }

        RunStartResponse(started = true, runId = runId)
    }

    fun getProgress(): RunProgress {
        val manifest = runRepository.currentManifest()
            ?: return RunProgress(status = "unknown", progressPct = 0.0)

        val (newLines, newCursor) = runRepository.newLogLinesSince(manifest.runId, manifest.logCursor)
        if (newCursor != manifest.logCursor) {
            runRepository.save(manifest.copy(logCursor = newCursor))
        }

        return RunProgress(
            status = manifest.status,
            progressPct = manifest.progressPct,
            steps = manifest.steps,
            newLogLines = newLines,
            runId = manifest.runId,
            error = manifest.error
        )
    }

    /** Re-enters an in-flight run after a server restart, picking up at its last recorded stage. */
    fun resumeIfNeeded() {
        val manifest = runRepository.currentManifest() ?: return
        if (manifest.status != RunStatus.RUNNING) return
        val resumeStage = Stage.entries.firstOrNull { it.slug == manifest.currentStage } ?: Stage.RESEARCH
        log(manifest.runId, "[warn] resuming run ${manifest.runId} from ${resumeStage.slug} after restart")
        scope.launch { executeStages(manifest.runId, resumeStage) }
    }

    private suspend fun executeStages(runId: String, startStage: Stage) {
        var stage: Stage? = startStage
        while (stage != null) {
            when (val outcome = runStage(runId, stage)) {
                is StageOutcome.Continue -> stage = stage.next()
                is StageOutcome.RetryFrom -> {
                    log(runId, "[warn] retrying from ${outcome.stage.slug}: ${outcome.reason}")
                    stage = outcome.stage
                }
                is StageOutcome.Fatal -> {
                    markError(runId, outcome.reason)
                    return
                }
            }
        }
        markDone(runId)
    }

    private suspend fun runStage(runId: String, stage: Stage): StageOutcome {
        updateStep(runId, stage, status = "running", detail = "")
        log(runId, "[info] ${stage.slug} started")

        // Stub: replaced by real department logic (Research/Design/Completion/Polish/Shipping).
        delay(2000)

        updateStep(runId, stage, status = "done", detail = "stub complete")
        log(runId, "[done] ${stage.slug} complete")
        return StageOutcome.Continue
    }

    private fun updateStep(runId: String, stage: Stage, status: String, detail: String) {
        runRepository.load(runId)?.let { manifest ->
            val updatedSteps = manifest.steps.map { step ->
                if (step.name == stage.slug) step.copy(status = status, detail = detail) else step
            }
            runRepository.save(manifest.copy(currentStage = stage.slug, steps = updatedSteps))
        }
    }

    private fun markDone(runId: String) {
        runRepository.load(runId)?.let { manifest ->
            runRepository.save(manifest.copy(status = RunStatus.DONE, internalStatus = InternalStatus.DONE))
        }
        log(runId, "[done] run $runId complete")
    }

    private fun markError(runId: String, reason: String) {
        runRepository.load(runId)?.let { manifest ->
            runRepository.save(manifest.copy(status = RunStatus.ERROR, error = reason))
        }
        log(runId, "[error] $reason")
    }

    private fun log(runId: String, line: String) {
        runRepository.appendLog(runId, line)
    }
}
