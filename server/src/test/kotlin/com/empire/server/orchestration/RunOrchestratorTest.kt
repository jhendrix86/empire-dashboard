package com.empire.server.orchestration

import com.empire.dashboard.data.RunRequest
import com.empire.server.llm.BudgetGuard
import com.empire.server.llm.CostTrackingLlmClient
import com.empire.server.llm.LlmClient
import com.empire.server.orchestration.stages.CompletionStage
import com.empire.server.orchestration.stages.DesignStage
import com.empire.server.orchestration.stages.PolishStage
import com.empire.server.orchestration.stages.ResearchStage
import com.empire.server.orchestration.stages.ShippingStage
import com.empire.server.storage.NicheRepository
import com.empire.server.storage.RunRepository
import com.empire.server.testutil.FakeLlmClient
import com.empire.server.testutil.happyPathResponder
import java.nio.file.Files
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Exercises the real stage classes end to end through a fake, in-memory [LlmClient] --
 * no network calls -- so these run fast and deterministically in CI. Uses [runBlocking]
 * rather than kotlinx-coroutines-test's virtual-time `runTest`: RunOrchestrator launches
 * its stage work on a real background [kotlinx.coroutines.Dispatchers.Default] scope
 * independent of the test's coroutine, so the polling loop below needs real wall-clock
 * delays for that background work to actually progress.
 */
class RunOrchestratorTest {

    private fun newOrchestrator(
        llm: LlmClient,
        runRepository: RunRepository,
        budgetGuard: BudgetGuard = BudgetGuard(maxCostUsd = null)
    ): RunOrchestrator {
        val nicheRepository = NicheRepository(Files.createTempDirectory("empire-test").toFile())
        return RunOrchestrator(
            runRepository = runRepository,
            researchStage = ResearchStage(llm, nicheRepository, runRepository),
            designStage = DesignStage(llm, runRepository),
            completionStage = CompletionStage(llm, runRepository),
            polishStage = PolishStage(llm, runRepository),
            shippingStage = ShippingStage(llm, runRepository),
            budgetGuard = budgetGuard
        )
    }

    private suspend fun awaitTerminal(runRepository: RunRepository, runId: String, timeoutSeconds: Int = 10): RunManifest {
        repeat(timeoutSeconds * 20) {
            val manifest = runRepository.load(runId)
            if (manifest != null && manifest.status != RunStatus.RUNNING) return manifest
            delay(50)
        }
        error("run $runId did not reach a terminal state within ${timeoutSeconds}s")
    }

    @Test
    fun `startRun rejects a second run while one is already in progress`() = runBlocking {
        val runRepository = RunRepository(Files.createTempDirectory("empire-test").toFile())
        runRepository.save(RunManifest(runId = "existing", createdAt = "now", status = RunStatus.RUNNING))
        runRepository.setCurrentRunId("existing")

        val orchestrator = newOrchestrator(FakeLlmClient(), runRepository)
        val response = orchestrator.startRun(RunRequest())

        assertFalse(response.started)
        assertEquals("a run is already in progress", response.error)
    }

    @Test
    fun `a full run with well-formed LLM responses completes`() = runBlocking {
        val runRepository = RunRepository(Files.createTempDirectory("empire-test").toFile())
        val orchestrator = newOrchestrator(FakeLlmClient(happyPathResponder()), runRepository)

        val response = orchestrator.startRun(RunRequest())
        assertTrue(response.started)

        val manifest = awaitTerminal(runRepository, response.runId)

        assertEquals(RunStatus.DONE, manifest.status)
        assertTrue(manifest.steps.all { it.status == "done" })
    }

    @Test
    fun `a run aborts once it crosses its LLM budget cap`() = runBlocking {
        val runRepository = RunRepository(Files.createTempDirectory("empire-test").toFile())
        val guard = BudgetGuard(maxCostUsd = 0.000001)
        val budgetedLlm = CostTrackingLlmClient(FakeLlmClient(happyPathResponder()), model = "claude-opus-5", guard = guard)
        val orchestrator = newOrchestrator(budgetedLlm, runRepository, guard)

        val response = orchestrator.startRun(RunRequest())
        assertTrue(response.started)

        val manifest = awaitTerminal(runRepository, response.runId)

        assertEquals(RunStatus.ERROR, manifest.status)
        assertTrue(manifest.error.orEmpty().contains("budget"))
    }
}
