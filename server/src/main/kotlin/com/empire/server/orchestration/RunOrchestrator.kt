package com.empire.server.orchestration

import com.empire.dashboard.data.RunProgress
import com.empire.dashboard.data.RunRequest
import com.empire.dashboard.data.RunStartResponse
import com.empire.server.orchestration.stages.CompletionStage
import com.empire.server.orchestration.stages.DesignStage
import com.empire.server.orchestration.stages.PolishStage
import com.empire.server.orchestration.stages.ResearchStage
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

class RunOrchestrator(
    private val runRepository: RunRepository,
    private val researchStage: ResearchStage,
    private val designStage: DesignStage,
    private val completionStage: CompletionStage,
    private val polishStage: PolishStage
) {
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
        val runId = runRepository.currentRunId()
            ?: return RunProgress(status = "unknown", progressPct = 0.0)

        var newLines: List<String> = emptyList()
        val manifest = runRepository.update(runId) { current ->
            val (lines, newCursor) = runRepository.newLogLinesSince(current.runId, current.logCursor)
            newLines = lines
            if (newCursor != current.logCursor) current.copy(logCursor = newCursor) else current
        } ?: return RunProgress(status = "unknown", progressPct = 0.0)

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

        val manifest = runRepository.load(runId)
            ?: return StageOutcome.Fatal("manifest missing for $runId")

        val result = try {
            when (stage) {
                Stage.RESEARCH -> researchStage.run(runId, manifest)
                Stage.PRODUCT_DESIGN -> designStage.run(runId, manifest)
                Stage.PRODUCT_COMPLETION -> completionStage.run(runId, manifest)
                Stage.POLISH_AUDIT -> polishStage.run(runId, manifest)
                Stage.SHIPPING -> {
                    // Stub: replaced by real Shipping stage logic.
                    delay(2000)
                    StageResult(StageOutcome.Continue, detail = "stub complete")
                }
            }
        } catch (e: Exception) {
            log(runId, "[error] ${stage.slug} failed: ${e.message}")
            updateStep(runId, stage, status = "error", detail = e.message ?: "failed")
            return StageOutcome.Fatal("${stage.slug} failed: ${e.message}")
        }

        when (result.outcome) {
            is StageOutcome.Continue -> {
                updateStep(runId, stage, status = "done", detail = result.detail)
                log(runId, "[done] ${stage.slug} complete")
            }
            is StageOutcome.RetryFrom -> {
                // Step statuses were already reset by the stage itself (see PolishStage.resetFrom);
                // executeStages() logs the retry, so nothing further to record here.
            }
            is StageOutcome.Fatal -> {
                updateStep(runId, stage, status = "error", detail = result.detail)
            }
        }
        return result.outcome
    }

    private fun updateStep(runId: String, stage: Stage, status: String, detail: String) {
        runRepository.update(runId) { manifest ->
            val updatedSteps = manifest.steps.map { step ->
                if (step.name == stage.slug) step.copy(status = status, detail = detail) else step
            }
            manifest.copy(currentStage = stage.slug, steps = updatedSteps)
        }
    }

    private fun markDone(runId: String) {
        runRepository.update(runId) { manifest ->
            manifest.copy(status = RunStatus.DONE, internalStatus = InternalStatus.DONE)
        }
        log(runId, "[done] run $runId complete")
    }

    private fun markError(runId: String, reason: String) {
        runRepository.update(runId) { manifest ->
            manifest.copy(status = RunStatus.ERROR, error = reason)
        }
        log(runId, "[error] $reason")
    }

    private fun log(runId: String, line: String) {
        runRepository.appendLog(runId, line)
    }
}
