package com.empire.server.routes

import com.empire.dashboard.data.EmpireStatus
import com.empire.dashboard.data.RunEntry
import com.empire.server.orchestration.RunStatus
import com.empire.server.storage.NicheRepository
import com.empire.server.storage.RunRepository
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import java.time.Instant

fun Route.statusRoutes(runRepository: RunRepository, nicheRepository: NicheRepository) {
    get("/status") {
        val recentRuns = runRepository.recentRuns()
        val latest = recentRuns.firstOrNull()

        val launchStep = when {
            latest == null -> "unknown"
            latest.status == RunStatus.RUNNING -> latest.currentStage
            latest.status == RunStatus.DONE -> "executed"
            else -> latest.status
        }

        val status = EmpireStatus(
            serverTime = Instant.now().toString(),
            lastPipelineRun = latest?.runId,
            lastRunAt = latest?.createdAt,
            launchStep = launchStep,
            selectedNiche = latest?.niche,
            topNiches = nicheRepository.all().sortedByDescending { it.score },
            bundle = latest?.bundle,
            recentPipelines = recentRuns.map { RunEntry(runId = it.runId, date = it.createdAt) },
            recentBundles = recentRuns.mapNotNull { run ->
                run.bundle?.let { bundle -> RunEntry(runId = run.runId, date = bundle.generatedAt) }
            }
        )
        call.respond(status)
    }

    get("/niche-scores") {
        call.respond(nicheRepository.all().sortedByDescending { it.score })
    }

    get("/runs") {
        call.respond(runRepository.recentRuns().map { RunEntry(runId = it.runId, date = it.createdAt) })
    }
}
