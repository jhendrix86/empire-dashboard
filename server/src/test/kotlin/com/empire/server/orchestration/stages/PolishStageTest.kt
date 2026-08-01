package com.empire.server.orchestration.stages

import com.empire.dashboard.data.SelectedNiche
import com.empire.server.llm.Personas
import com.empire.server.orchestration.InternalStatus
import com.empire.server.orchestration.RunManifest
import com.empire.server.orchestration.Stage
import com.empire.server.orchestration.StageOutcome
import com.empire.server.orchestration.artifacts.AuditReport
import com.empire.server.orchestration.artifacts.DeliverableManifest
import com.empire.server.orchestration.artifacts.EnterpriseBlueprint
import com.empire.server.orchestration.artifacts.ResearchBrief
import com.empire.server.orchestration.readArtifact
import com.empire.server.orchestration.writeArtifact
import com.empire.server.storage.RunRepository
import com.empire.server.testutil.FakeLlmClient
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PolishStageTest {
    private val niche = SelectedNiche(niche = "N", subNiche = "S", audience = "A", coreProblem = "P")

    private fun newRepo(): RunRepository = RunRepository(Files.createTempDirectory("empire-test").toFile())

    private fun seedArtifacts(repo: RunRepository, runId: String) {
        writeArtifact(
            repo, runId, "research-brief.json", ResearchBrief.serializer(),
            ResearchBrief(
                niche = niche,
                audienceProfile = "profile",
                coreProblemDetail = "problem",
                competitiveLandscape = "landscape",
                legalComplianceNotes = "notes",
                monetizationAngle = "angle",
                recommendedProductFormat = "ebook"
            )
        )
        writeArtifact(
            repo, runId, "blueprint.json", EnterpriseBlueprint.serializer(),
            EnterpriseBlueprint(
                productOutline = "outline",
                brandVoiceGuide = "voice",
                visualDirection = "visual",
                pricing = "9.99",
                leadMagnetConcept = "lead",
                funnelDesign = "funnel",
                outputFormats = listOf("ebook"),
                platformTargets = listOf("gumroad")
            )
        )
        writeArtifact(
            repo, runId, "deliverable-manifest.json", DeliverableManifest.serializer(),
            DeliverableManifest(files = emptyList(), leadMagnetFileName = "lead-magnet.md")
        )
    }

    /** Maps each audit persona to the check name PolishStage records it under, per Personas.kt. */
    private fun checkResultResponder(failing: Set<String>): (String, String) -> String = { system, _ ->
        val name = when (system) {
            Personas.COPYEDITOR_PROOFREADER -> "copyedit"
            Personas.BRAND_CONSISTENCY_AUDITOR -> "brand-consistency"
            Personas.LEGAL_COMPLIANCE_REVIEWER -> "legal-compliance"
            Personas.ACCESSIBILITY_EDITORIAL_REVIEWER -> "accessibility-editorial"
            Personas.GAP_ANALYST -> "gap-analysis"
            else -> error("unexpected persona in polish stage: $system")
        }
        val pass = name !in failing
        """{"pass": $pass, "notes": "note for $name"}"""
    }

    @Test
    fun `all checks passing continues the pipeline`() = runBlocking {
        val repo = newRepo()
        val runId = "run-1"
        repo.save(RunManifest(runId = runId, createdAt = "now"))
        seedArtifacts(repo, runId)

        val stage = PolishStage(FakeLlmClient(checkResultResponder(emptySet())), repo)
        val result = stage.run(runId, repo.load(runId)!!)

        assertEquals(StageOutcome.Continue, result.outcome)
        val report = readArtifact(repo, runId, "audit-report.json", AuditReport.serializer())
        assertEquals(true, report?.pass)
    }

    @Test
    fun `a brand or legal failure routes the retry back to design`() = runBlocking {
        val repo = newRepo()
        val runId = "run-1"
        repo.save(RunManifest(runId = runId, createdAt = "now"))
        seedArtifacts(repo, runId)

        val stage = PolishStage(FakeLlmClient(checkResultResponder(setOf("brand-consistency"))), repo)
        val result = stage.run(runId, repo.load(runId)!!)

        val outcome = assertIs<StageOutcome.RetryFrom>(result.outcome)
        assertEquals(Stage.PRODUCT_DESIGN, outcome.stage)
        assertEquals(1, repo.load(runId)?.retryCount)
    }

    @Test
    fun `a copyedit-only failure routes the retry back to completion`() = runBlocking {
        val repo = newRepo()
        val runId = "run-1"
        repo.save(RunManifest(runId = runId, createdAt = "now"))
        seedArtifacts(repo, runId)

        val stage = PolishStage(FakeLlmClient(checkResultResponder(setOf("copyedit"))), repo)
        val result = stage.run(runId, repo.load(runId)!!)

        val outcome = assertIs<StageOutcome.RetryFrom>(result.outcome)
        assertEquals(Stage.PRODUCT_COMPLETION, outcome.stage)
    }

    @Test
    fun `exhausting retries marks the run as needing attention`() = runBlocking {
        val repo = newRepo()
        val runId = "run-1"
        repo.save(RunManifest(runId = runId, createdAt = "now", retryCount = MAX_AUDIT_RETRIES))
        seedArtifacts(repo, runId)

        val stage = PolishStage(FakeLlmClient(checkResultResponder(setOf("copyedit"))), repo)
        val result = stage.run(runId, repo.load(runId)!!)

        assertIs<StageOutcome.Fatal>(result.outcome)
        assertEquals(InternalStatus.NEEDS_ATTENTION, repo.load(runId)?.internalStatus)
    }
}
