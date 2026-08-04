package com.empire.server.orchestration.stages

import com.empire.dashboard.data.SelectedNiche
import com.empire.server.llm.Personas
import com.empire.server.orchestration.RunManifest
import com.empire.server.orchestration.artifacts.DeliverableManifest
import com.empire.server.orchestration.artifacts.EnterpriseBlueprint
import com.empire.server.orchestration.artifacts.ResearchBrief
import com.empire.server.orchestration.readArtifact
import com.empire.server.orchestration.writeArtifact
import com.empire.server.storage.RunRepository
import com.empire.server.testutil.FakeLlmClient
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompletionStageTest {
    private fun newRepo(): RunRepository = RunRepository(Files.createTempDirectory("empire-test").toFile())

    private fun seedArtifacts(repo: RunRepository, runId: String, outputFormats: List<String>, platformTargets: List<String>) {
        writeArtifact(
            repo, runId, "research-brief.json", ResearchBrief.serializer(),
            ResearchBrief(
                niche = SelectedNiche(niche = "N", subNiche = "S"),
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
                outputFormats = outputFormats,
                platformTargets = platformTargets
            )
        )
    }

    private fun responder(): suspend (String, String) -> String = { system, _ ->
        when (system) {
            Personas.WEB_APP_SCAFFOLDER -> "<html>app content</html>"
            Personas.EBOOK_AUTHOR -> "# ebook content"
            Personas.MARKETPLACE_LISTING_COPYWRITER -> "listing copy"
            Personas.LEAD_MAGNET_WRITER -> "lead magnet content"
            else -> "placeholder"
        }
    }

    @Test
    fun `an app-ish format is written as html via the web-app scaffolder persona`() = runBlocking {
        val repo = newRepo()
        val runId = "run-1"
        repo.save(RunManifest(runId = runId, createdAt = "now"))
        seedArtifacts(repo, runId, outputFormats = listOf("web-app"), platformTargets = emptyList())

        CompletionStage(FakeLlmClient(responder()), repo).run(runId, repo.load(runId)!!)

        val file = File(repo.runDir(runId), "deliverables/product-web-app.html")
        assertTrue(file.exists())
        assertEquals("<html>app content</html>", file.readText())
    }

    @Test
    fun `a non-app format is written as markdown via the ebook author persona`() = runBlocking {
        val repo = newRepo()
        val runId = "run-1"
        repo.save(RunManifest(runId = runId, createdAt = "now"))
        seedArtifacts(repo, runId, outputFormats = listOf("ebook"), platformTargets = emptyList())

        CompletionStage(FakeLlmClient(responder()), repo).run(runId, repo.load(runId)!!)

        val file = File(repo.runDir(runId), "deliverables/product-ebook.md")
        assertTrue(file.exists())
        assertEquals("# ebook content", file.readText())
    }

    @Test
    fun `writes a listing file per platform target and a lead magnet, recorded in the manifest`() = runBlocking {
        val repo = newRepo()
        val runId = "run-1"
        repo.save(RunManifest(runId = runId, createdAt = "now"))
        seedArtifacts(repo, runId, outputFormats = listOf("ebook"), platformTargets = listOf("gumroad", "Etsy Shop"))

        CompletionStage(FakeLlmClient(responder()), repo).run(runId, repo.load(runId)!!)

        val deliverablesDir = File(repo.runDir(runId), "deliverables")
        assertTrue(File(deliverablesDir, "listing-gumroad.md").exists())
        assertTrue(File(deliverablesDir, "listing-etsy-shop.md").exists())
        assertEquals("lead magnet content", File(deliverablesDir, "lead-magnet.md").readText())

        val manifest = readArtifact(repo, runId, "deliverable-manifest.json", DeliverableManifest.serializer())
        assertEquals("lead-magnet.md", manifest?.leadMagnetFileName)
        // 1 product file + 1 listing per platform target
        assertEquals(
            setOf("product-ebook.md", "listing-gumroad.md", "listing-etsy-shop.md"),
            manifest?.files?.map { it.fileName }?.toSet()
        )
    }
}
