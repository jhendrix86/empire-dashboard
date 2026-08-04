package com.empire.server.orchestration.stages

import com.empire.dashboard.data.SelectedNiche
import com.empire.server.llm.Personas
import com.empire.server.orchestration.RunManifest
import com.empire.server.orchestration.StageOutcome
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

class DesignStageTest {
    private fun newRepo(): RunRepository = RunRepository(Files.createTempDirectory("empire-test").toFile())

    private fun seedBrief(repo: RunRepository, runId: String) {
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
    }

    private fun responder(
        formatDecisionRaw: String,
        voiceAndVisualRaw: String = "VOICE: friendly.\nVISUAL: clean.",
        leadMagnetAndFunnelRaw: String = "LEAD MAGNET: a checklist.\nFUNNEL: three emails."
    ): suspend (String, String) -> String = { system, _ ->
        when (system) {
            Personas.FORMAT_PLATFORM_DECISION_MAKER -> formatDecisionRaw
            Personas.BRAND_VOICE_VISUAL_DIRECTOR -> voiceAndVisualRaw
            Personas.LEAD_MAGNET_FUNNEL_DESIGNER -> leadMagnetAndFunnelRaw
            else -> "placeholder"
        }
    }

    @Test
    fun `falls back to ebook on gumroad when the format decision is unparseable`() = runBlocking {
        val repo = newRepo()
        val runId = "run-1"
        repo.save(RunManifest(runId = runId, createdAt = "now"))
        seedBrief(repo, runId)

        val result = DesignStage(FakeLlmClient(responder(formatDecisionRaw = "not json")), repo).run(runId, repo.load(runId)!!)

        assertIs<StageOutcome.Continue>(result.outcome)
        val blueprint = readArtifact(repo, runId, "blueprint.json", EnterpriseBlueprint.serializer())
        assertEquals(listOf("ebook"), blueprint?.outputFormats)
        assertEquals(listOf("gumroad"), blueprint?.platformTargets)
    }

    @Test
    fun `parses a well-formed format decision`() = runBlocking {
        val repo = newRepo()
        val runId = "run-1"
        repo.save(RunManifest(runId = runId, createdAt = "now"))
        seedBrief(repo, runId)

        val json = """{"outputFormats": ["web-app", "guide"], "platformTargets": ["etsy", "shopify"]}"""
        DesignStage(FakeLlmClient(responder(formatDecisionRaw = json)), repo).run(runId, repo.load(runId)!!)

        val blueprint = readArtifact(repo, runId, "blueprint.json", EnterpriseBlueprint.serializer())
        assertEquals(listOf("web-app", "guide"), blueprint?.outputFormats)
        assertEquals(listOf("etsy", "shopify"), blueprint?.platformTargets)
    }

    @Test
    fun `falls back to the raw text for both halves when VOICE-VISUAL labels are missing`() = runBlocking {
        val repo = newRepo()
        val runId = "run-1"
        repo.save(RunManifest(runId = runId, createdAt = "now"))
        seedBrief(repo, runId)

        val unlabeledResponse = "Just a friendly, clean brand description with no labels at all."
        val json = """{"outputFormats": ["ebook"], "platformTargets": ["gumroad"]}"""
        DesignStage(FakeLlmClient(responder(formatDecisionRaw = json, voiceAndVisualRaw = unlabeledResponse)), repo)
            .run(runId, repo.load(runId)!!)

        val blueprint = readArtifact(repo, runId, "blueprint.json", EnterpriseBlueprint.serializer())
        assertEquals(unlabeledResponse, blueprint?.brandVoiceGuide)
        assertEquals(unlabeledResponse, blueprint?.visualDirection)
    }

    @Test
    fun `splits VOICE-VISUAL sections correctly when labels are present`() = runBlocking {
        val repo = newRepo()
        val runId = "run-1"
        repo.save(RunManifest(runId = runId, createdAt = "now"))
        seedBrief(repo, runId)

        val json = """{"outputFormats": ["ebook"], "platformTargets": ["gumroad"]}"""
        DesignStage(
            FakeLlmClient(responder(formatDecisionRaw = json, voiceAndVisualRaw = "VOICE: warm and direct.\nVISUAL: minimal, high contrast.")),
            repo
        ).run(runId, repo.load(runId)!!)

        val blueprint = readArtifact(repo, runId, "blueprint.json", EnterpriseBlueprint.serializer())
        assertEquals("warm and direct.", blueprint?.brandVoiceGuide)
        assertEquals("minimal, high contrast.", blueprint?.visualDirection)
    }
}
