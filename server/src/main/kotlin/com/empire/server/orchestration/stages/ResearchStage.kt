package com.empire.server.orchestration.stages

import com.empire.dashboard.data.SelectedNiche
import com.empire.server.llm.AnthropicClient
import com.empire.server.llm.Personas
import com.empire.server.llm.extractJsonObject
import com.empire.server.orchestration.RunManifest
import com.empire.server.orchestration.StageOutcome
import com.empire.server.orchestration.StageResult
import com.empire.server.orchestration.artifacts.ResearchBrief
import com.empire.server.orchestration.writeArtifact
import com.empire.server.storage.NicheRepository
import com.empire.server.storage.RunRepository
import com.empire.server.storage.appJson

class ResearchStage(
    private val llm: AnthropicClient,
    private val nicheRepository: NicheRepository,
    private val runRepository: RunRepository
) {
    suspend fun run(runId: String, manifest: RunManifest): StageResult {
        val niche = manifest.niche ?: originateNiche().also { originated ->
            nicheRepository.add(originated)
            // Persist to the manifest immediately, before the slow LLM calls below --
            // otherwise a restart mid-research finds manifest.niche still null and
            // originates (and records) a second, duplicate niche on resume.
            runRepository.update(runId) { it.copy(niche = originated) }
        }
        val nicheSummary = summarize(niche)

        val audienceProfile = llm.complete(Personas.AUDIENCE_PERSONA_RESEARCHER, nicheSummary)
        val competitiveLandscape = llm.complete(Personas.COMPETITIVE_ANALYST, nicheSummary)
        val legalNotes = llm.complete(Personas.LEGAL_COMPLIANCE_RESEARCHER, nicheSummary)
        val monetizationAndFormat = llm.complete(
            Personas.MARKET_TREND_ANALYST,
            "$nicheSummary\n\nRecommend a monetization angle and the single best product format. " +
                "Respond in exactly two lines of plain text, nothing else."
        )

        val brief = ResearchBrief(
            niche = niche,
            audienceProfile = audienceProfile,
            coreProblemDetail = niche.coreProblem,
            competitiveLandscape = competitiveLandscape,
            legalComplianceNotes = legalNotes,
            monetizationAngle = monetizationAndFormat,
            recommendedProductFormat = monetizationAndFormat
        )

        writeArtifact(runRepository, runId, "research-brief.json", ResearchBrief.serializer(), brief)

        return StageResult(StageOutcome.Continue, detail = "brief ready for ${niche.subNiche}")
    }

    private suspend fun originateNiche(): SelectedNiche {
        val raw = llm.complete(
            Personas.MARKET_TREND_ANALYST,
            "Identify one promising, narrow digital-product niche right now and score it."
        )
        return runCatching {
            appJson.decodeFromString(SelectedNiche.serializer(), extractJsonObject(raw))
        }.getOrElse {
            SelectedNiche(
                niche = "General self-improvement",
                subNiche = "Productivity for freelancers",
                audience = "Freelancers struggling with time management",
                coreProblem = "Inconsistent income due to poor time allocation",
                demand = 50.0,
                competition = 50.0,
                speed = 50.0,
                legalRisk = 10.0,
                brandFit = 50.0,
                score = 50.0
            )
        }
    }

    private fun summarize(niche: SelectedNiche): String = """
        Niche: ${niche.niche}
        SubNiche: ${niche.subNiche}
        Audience: ${niche.audience}
        CoreProblem: ${niche.coreProblem}
    """.trimIndent()
}
