package com.empire.server.routes

import com.empire.dashboard.data.RunRequest
import com.empire.server.orchestration.RunOrchestrator
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.pipelineRoutes(orchestrator: RunOrchestrator) {
    post("/run") {
        val body = call.receive<RunRequest>()
        call.respond(orchestrator.startRun(body))
    }

    get("/run-progress") {
        call.respond(orchestrator.getProgress())
    }
}
