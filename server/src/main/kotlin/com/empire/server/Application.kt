package com.empire.server

import com.empire.server.config.AppConfig
import com.empire.server.llm.AnthropicClient
import com.empire.server.orchestration.RunOrchestrator
import com.empire.server.orchestration.stages.CompletionStage
import com.empire.server.orchestration.stages.DesignStage
import com.empire.server.orchestration.stages.PolishStage
import com.empire.server.orchestration.stages.ResearchStage
import com.empire.server.orchestration.stages.ShippingStage
import com.empire.server.routes.customerRoutes
import com.empire.server.routes.leadRoutes
import com.empire.server.routes.pipelineRoutes
import com.empire.server.routes.revenueRoutes
import com.empire.server.routes.statusRoutes
import com.empire.server.storage.CustomerRepository
import com.empire.server.storage.LeadRepository
import com.empire.server.storage.NicheRepository
import com.empire.server.storage.RevenueRepository
import com.empire.server.storage.RunRepository
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.uri
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

fun main() {
    check(!AppConfig.bindAllInterfaces || !AppConfig.authToken.isNullOrBlank()) {
        "EMPIRE_BIND_ALL=true exposes mutating endpoints on the network; " +
            "set EMPIRE_AUTH_TOKEN before enabling it."
    }
    val host = if (AppConfig.bindAllInterfaces) "0.0.0.0" else "127.0.0.1"
    embeddedServer(Netty, port = 8765, host = host, module = Application::module).start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            }
        )
    }
    install(CallLogging) {
        level = Level.INFO
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled exception handling ${call.request.uri}", cause)
            call.respondText(
                text = "Internal server error",
                status = HttpStatusCode.InternalServerError
            )
        }
    }

    val runRepository = RunRepository()
    val nicheRepository = NicheRepository()
    val customerRepository = CustomerRepository()
    val leadRepository = LeadRepository()
    val revenueRepository = RevenueRepository()

    val llm = AnthropicClient()
    val orchestrator = RunOrchestrator(
        runRepository = runRepository,
        researchStage = ResearchStage(llm, nicheRepository, runRepository),
        designStage = DesignStage(llm, runRepository),
        completionStage = CompletionStage(llm, runRepository),
        polishStage = PolishStage(llm, runRepository),
        shippingStage = ShippingStage(llm, runRepository)
    )
    orchestrator.resumeIfNeeded()

    routing {
        get("/health") {
            call.respondText("OK")
        }
        statusRoutes(runRepository, nicheRepository)
        customerRoutes(customerRepository)
        leadRoutes(leadRepository)
        revenueRoutes(revenueRepository)
        pipelineRoutes(orchestrator)
    }
}
