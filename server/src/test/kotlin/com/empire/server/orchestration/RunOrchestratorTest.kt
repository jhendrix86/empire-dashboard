package com.empire.server.orchestration

import com.empire.dashboard.data.RunRequest
import com.empire.server.llm.BudgetGuard
import com.empire.server.llm.CostTrackingLlmClient
import com.empire.server.llm.LlmClient
import com.empire.server.notify.Notifier
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
        budgetGuard: BudgetGuard = BudgetGuard(maxCostUsd = null),
        notifier: Notifier = FakeNotifier()
    ): RunOrchestrator {
        val nicheRepository = NicheRepository(Files.createTempDirectory("empire-test").toFile())
        return RunOrchestrator(
            runRepository = runRepository,
            researchStage = ResearchStage(llm, nicheRepository, runRepository),
            designStage = DesignStage(llm, runRepository),
            completionStage = CompletionStage(llm, runRepository),
            polishStage = PolishStage(llm, runRepository),
            shippingStage = ShippingStage(llm, runRepository),
            budgetGuard = budgetGuard,
            notifier = notifier
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

    @Test
    fun `spend persisted mid-run lets a restart's fresh BudgetGuard pick up where it left off`() = runBlocking {
        val runRepository = RunRepository(Files.createTempDirectory("empire-test").toFile())
        val cap = 0.000001

        // Mirrors how Application.module() wires BudgetGuard: persist the running total onto
        // whichever run is current, so a restart doesn't hand the resumed run a fresh cap.
        fun persistingGuard(): BudgetGuard = BudgetGuard(maxCostUsd = cap) { spent ->
            runRepository.currentRunId()?.let { runId -> runRepository.update(runId) { it.copy(spentUsd = spent) } }
        }

        val firstGuard = persistingGuard()
        val firstLlm = CostTrackingLlmClient(FakeLlmClient(happyPathResponder()), model = "claude-opus-5", guard = firstGuard)
        val response = newOrchestrator(firstLlm, runRepository, firstGuard).startRun(RunRequest())
        val afterFirstAttempt = awaitTerminal(runRepository, response.runId)

        assertEquals(RunStatus.ERROR, afterFirstAttempt.status)
        assertTrue(afterFirstAttempt.spentUsd > 0.0)

        // Simulate a server restart mid-run: the manifest is still RUNNING (as if the crash
        // happened before markError ran), and Application.module() would construct a brand
        // new, zeroed BudgetGuard on the new process -- only resumeIfNeeded's seed() call
        // stands between that and a fresh full budget.
        runRepository.update(response.runId) { it.copy(status = RunStatus.RUNNING, error = null) }
        val secondGuard = persistingGuard()
        val secondLlm = CostTrackingLlmClient(FakeLlmClient(happyPathResponder()), model = "claude-opus-5", guard = secondGuard)
        newOrchestrator(secondLlm, runRepository, secondGuard).resumeIfNeeded()

        val afterResume = awaitTerminal(runRepository, response.runId)

        assertEquals(RunStatus.ERROR, afterResume.status)
        assertTrue(afterResume.error.orEmpty().contains("budget"))
    }

    @Test
    fun `cancelRun stops an in-flight run and marks it cancelled`() = runBlocking {
        val runRepository = RunRepository(Files.createTempDirectory("empire-test").toFile())
        // Never resolves on its own -- only cancellation ends it, so a passing test proves
        // cancelRun() actually interrupted the in-flight "LLM call" rather than the run
        // just finishing naturally in the meantime.
        val llm = FakeLlmClient { _, _ ->
            delay(60_000)
            error("should have been cancelled before this resolved")
        }
        val orchestrator = newOrchestrator(llm, runRepository)

        val response = orchestrator.startRun(RunRequest())
        assertTrue(response.started)
        delay(100) // let the background coroutine actually enter the delay

        val cancelResult = orchestrator.cancelRun()
        assertTrue(cancelResult.cancelled)

        val manifest = awaitTerminal(runRepository, response.runId)
        assertEquals(RunStatus.CANCELLED, manifest.status)
    }

    @Test
    fun `cancelRun is a no-op when nothing is running`() = runBlocking {
        val runRepository = RunRepository(Files.createTempDirectory("empire-test").toFile())
        val orchestrator = newOrchestrator(FakeLlmClient(), runRepository)

        val result = orchestrator.cancelRun()

        assertFalse(result.cancelled)
        assertEquals("no run in progress", result.error)
    }

    @Test
    fun `a failed run sends a run_failed notification`() = runBlocking {
        val runRepository = RunRepository(Files.createTempDirectory("empire-test").toFile())
        val notifier = FakeNotifier()
        val guard = BudgetGuard(maxCostUsd = 0.000001)
        val budgetedLlm = CostTrackingLlmClient(FakeLlmClient(happyPathResponder()), model = "claude-opus-5", guard = guard)
        val orchestrator = newOrchestrator(budgetedLlm, runRepository, guard, notifier)

        val response = orchestrator.startRun(RunRequest())
        awaitTerminal(runRepository, response.runId)

        assertEquals(1, notifier.sent.size)
        assertEquals("run_failed", notifier.sent.single().event)
        assertTrue(notifier.sent.single().message.contains(response.runId))
    }

    @Test
    fun `a successful run sends no notification`() = runBlocking {
        val runRepository = RunRepository(Files.createTempDirectory("empire-test").toFile())
        val notifier = FakeNotifier()
        val orchestrator = newOrchestrator(FakeLlmClient(happyPathResponder()), runRepository, notifier = notifier)

        val response = orchestrator.startRun(RunRequest())
        awaitTerminal(runRepository, response.runId)

        assertTrue(notifier.sent.isEmpty())
    }
}
