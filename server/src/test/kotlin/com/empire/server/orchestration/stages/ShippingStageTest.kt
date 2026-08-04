package com.empire.server.orchestration.stages

import com.empire.dashboard.data.SelectedNiche
import com.empire.server.orchestration.RunManifest
import com.empire.server.orchestration.StageOutcome
import com.empire.server.orchestration.artifacts.DeliverableFile
import com.empire.server.orchestration.artifacts.DeliverableManifest
import com.empire.server.orchestration.artifacts.EnterpriseBlueprint
import com.empire.server.orchestration.artifacts.ResearchBrief
import com.empire.server.orchestration.writeArtifact
import com.empire.server.storage.RunRepository
import com.empire.server.testutil.FakeLlmClient
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ShippingStageTest {
    private fun newRepo(): RunRepository = RunRepository(Files.createTempDirectory("empire-test").toFile())

    @Test
    fun `bundles the product deliverable, lead magnet, and launch instructions`() = runBlocking {
        val repo = newRepo()
        val runId = "run-1"
        repo.save(RunManifest(runId = runId, createdAt = "now"))

        writeArtifact(
            repo, runId, "research-brief.json", ResearchBrief.serializer(),
            ResearchBrief(
                niche = SelectedNiche(niche = "Pet care", subNiche = "Senior Dog Nutrition"),
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
                pricing = "19.00",
                leadMagnetConcept = "lead",
                funnelDesign = "funnel",
                outputFormats = listOf("ebook"),
                platformTargets = listOf("gumroad")
            )
        )
        val deliverableManifest = DeliverableManifest(
            files = listOf(DeliverableFile(format = "ebook", fileName = "product-ebook.md", description = "main")),
            leadMagnetFileName = "lead-magnet.md"
        )
        writeArtifact(repo, runId, "deliverable-manifest.json", DeliverableManifest.serializer(), deliverableManifest)

        val deliverablesDir = File(repo.runDir(runId), "deliverables").apply { mkdirs() }
        File(deliverablesDir, "product-ebook.md").writeText("the ebook")
        File(deliverablesDir, "lead-magnet.md").writeText("the lead magnet")

        val result = ShippingStage(FakeLlmClient { _, _ -> "launch instructions" }, repo).run(runId, repo.load(runId)!!)

        assertIs<StageOutcome.Continue>(result.outcome)
        val bundle = repo.load(runId)?.bundle
        assertEquals(true, bundle?.bundleExists)
        assertEquals("Senior Dog Nutrition", bundle?.productName)
        assertEquals(
            setOf("product-ebook.md", "lead-magnet.md", "instructions.md"),
            bundle?.copiedFiles?.toSet()
        )

        val instructionsFile = File(repo.runDir(runId), "bundle/instructions.md")
        assertTrue(instructionsFile.exists())
        assertEquals("launch instructions", instructionsFile.readText())
    }
}
