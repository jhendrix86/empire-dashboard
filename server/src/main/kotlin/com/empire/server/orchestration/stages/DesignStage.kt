package com.empire.server.orchestration.stages

import com.empire.server.llm.LlmClient
import com.empire.server.llm.Personas
import com.empire.server.llm.extractJsonObject
import com.empire.server.orchestration.RunManifest
import com.empire.server.orchestration.StageOutcome
import com.empire.server.orchestration.StageResult
import com.empire.server.orchestration.artifacts.EnterpriseBlueprint
import com.empire.server.orchestration.artifacts.FormatPlatformDecision
import com.empire.server.orchestration.artifacts.ResearchBrief
import com.empire.server.orchestration.readArtifact
import com.empire.server.orchestration.writeArtifact
import com.empire.server.storage.RunRepository
import com.empire.server.storage.appJson

class DesignStage(
    private val llm: LlmClient,
    private val runRepository: RunRepository
) {
    suspend fun run(runId: String, manifest: RunManifest): StageResult {
        val brief = readArtifact(runRepository, runId, "research-brief.json", ResearchBrief.serializer())
            ?: return StageResult(StageOutcome.Fatal("research brief missing for $runId"))

        val briefSummary = summarize(brief)

        val productOutline = llm.complete(Personas.PRODUCT_ARCHITECT, briefSummary)
        val pricing = llm.complete(Personas.PRICING_STRATEGIST, briefSummary)

        val voiceAndVisual = llm.complete(Personas.BRAND_VOICE_VISUAL_DIRECTOR, briefSummary)
        val (brandVoiceGuide, visualDirection) = splitTwoSections(voiceAndVisual, "VOICE:", "VISUAL:")

        val leadMagnetAndFunnel = llm.complete(Personas.LEAD_MAGNET_FUNNEL_DESIGNER, briefSummary)
        val (leadMagnetConcept, funnelDesign) = splitTwoSections(leadMagnetAndFunnel, "LEAD MAGNET:", "FUNNEL:")

        val formatDecisionRaw = llm.complete(
            Personas.FORMAT_PLATFORM_DECISION_MAKER,
            "$briefSummary\n\nRecommended format hint: ${brief.recommendedProductFormat}"
        )
        val formatDecision = runCatching {
            appJson.decodeFromString(FormatPlatformDecision.serializer(), extractJsonObject(formatDecisionRaw))
        }.getOrElse { FormatPlatformDecision(outputFormats = listOf("ebook"), platformTargets = listOf("gumroad")) }

        val blueprint = EnterpriseBlueprint(
            productOutline = productOutline,
            brandVoiceGuide = brandVoiceGuide,
            visualDirection = visualDirection,
            pricing = pricing,
            leadMagnetConcept = leadMagnetConcept,
            funnelDesign = funnelDesign,
            outputFormats = formatDecision.outputFormats,
            platformTargets = formatDecision.platformTargets
        )

        writeArtifact(runRepository, runId, "blueprint.json", EnterpriseBlueprint.serializer(), blueprint)

        return StageResult(
            StageOutcome.Continue,
            detail = "formats: ${blueprint.outputFormats.joinToString(", ")}"
        )
    }

    private fun summarize(brief: ResearchBrief): String = """
        Niche: ${brief.niche.niche} / ${brief.niche.subNiche}
        Audience profile: ${brief.audienceProfile}
        Core problem: ${brief.coreProblemDetail}
        Competitive landscape: ${brief.competitiveLandscape}
        Legal/compliance notes: ${brief.legalComplianceNotes}
        Monetization angle & recommended format: ${brief.monetizationAngle}
    """.trimIndent()

    /** Splits a two-section persona response on its labels; falls back to the whole text for both if not found. */
    private fun splitTwoSections(text: String, firstLabel: String, secondLabel: String): Pair<String, String> {
        val firstIdx = text.indexOf(firstLabel, ignoreCase = true)
        val secondIdx = text.indexOf(secondLabel, ignoreCase = true)
        if (firstIdx < 0 || secondIdx < 0 || secondIdx <= firstIdx) return text to text
        val first = text.substring(firstIdx + firstLabel.length, secondIdx).trim(':', ' ', '\n')
        val second = text.substring(secondIdx + secondLabel.length).trim(':', ' ', '\n')
        return first to second
    }
}
