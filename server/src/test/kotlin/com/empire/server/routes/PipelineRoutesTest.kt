package com.empire.server.routes

import com.empire.dashboard.data.RunCancelResponse
import com.empire.dashboard.data.RunProgress
import com.empire.dashboard.data.RunStartResponse
import com.empire.server.llm.BudgetGuard
import com.empire.server.llm.LlmClient
import com.empire.server.orchestration.RunOrchestrator
import com.empire.server.orchestration.stages.CompletionStage
import com.empire.server.orchestration.stages.DesignStage
import com.empire.server.orchestration.stages.PolishStage
import com.empire.server.orchestration.stages.ResearchStage
import com.empire.server.orchestration.stages.ShippingStage
import com.empire.server.storage.NicheRepository
import com.empire.server.storage.RunRepository
import com.empire.server.testutil.FakeLlmClient
import com.empire.server.testutil.FakeNotifier
import com.empire.server.testutil.happyPathResponder
import com.empire.server.testutil.testJson
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.install
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import java.nio.file.Files
import kotlinx.coroutines.delay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PipelineRoutesTest {
    private fun newOrchestrator(llm: LlmClient, runRepository: RunRepository): RunOrchestrator {
        val nicheRepository = NicheRepository(Files.createTempDirectory("empire-test").toFile())
        return RunOrchestrator(
            runRepository = runRepository,
            researchStage = ResearchStage(llm, nicheRepository, runRepository),
            designStage = DesignStage(llm, runRepository),
            completionStage = CompletionStage(llm, runRepository),
            polishStage = PolishStage(llm, runRepository),
            shippingStage = ShippingStage(llm, runRepository),
            budgetGuard = BudgetGuard(maxCostUsd = null),
            notifier = FakeNotifier()
        )
    }

    @Test
    fun `POST run starts a run and GET run-progress reports it running`() = testApplication {
        val runRepository = RunRepository(Files.createTempDirectory("empire-test").toFile())
        val orchestrator = newOrchestrator(FakeLlmClient(happyPathResponder()), runRepository)
        application {
            install(ContentNegotiation) { json(testJson) }
            routing { pipelineRoutes(orchestrator) }
        }

        val startResponse = client.post("/run") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }

        assertEquals(HttpStatusCode.OK, startResponse.status)
        val started = testJson.decodeFromString(RunStartResponse.serializer(), startResponse.bodyAsText())
        assertTrue(started.started)
        assertTrue(started.runId.isNotBlank())

        val progressResponse = client.get("/run-progress")
        val progress = testJson.decodeFromString(RunProgress.serializer(), progressResponse.bodyAsText())
        assertEquals(started.runId, progress.runId)
    }

    @Test
    fun `POST run-cancel stops an in-flight run started via POST run`() = testApplication {
        val runRepository = RunRepository(Files.createTempDirectory("empire-test").toFile())
        // Never resolves on its own -- only cancellation ends it.
        val llm = FakeLlmClient { _, _ -> delay(60_000); error("should have been cancelled first") }
        val orchestrator = newOrchestrator(llm, runRepository)
        application {
            install(ContentNegotiation) { json(testJson) }
            routing { pipelineRoutes(orchestrator) }
        }

        client.post("/run") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        delay(100) // let the background coroutine actually enter the delay

        val cancelResponse = client.post("/run/cancel")

        assertEquals(HttpStatusCode.OK, cancelResponse.status)
        val cancelled = testJson.decodeFromString(RunCancelResponse.serializer(), cancelResponse.bodyAsText())
        assertTrue(cancelled.cancelled)
    }

    @Test
    fun `POST run-cancel with nothing running reports an error, not an exception`() = testApplication {
        val runRepository = RunRepository(Files.createTempDirectory("empire-test").toFile())
        val orchestrator = newOrchestrator(FakeLlmClient(), runRepository)
        application {
            install(ContentNegotiation) { json(testJson) }
            routing { pipelineRoutes(orchestrator) }
        }

        val response = client.post("/run/cancel")

        assertEquals(HttpStatusCode.OK, response.status)
        val cancelled = testJson.decodeFromString(RunCancelResponse.serializer(), response.bodyAsText())
        assertEquals(false, cancelled.cancelled)
        assertEquals("no run in progress", cancelled.error)
    }
}
