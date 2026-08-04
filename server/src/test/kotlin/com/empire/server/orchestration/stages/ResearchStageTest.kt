package com.empire.server.orchestration.stages

import com.empire.dashboard.data.SelectedNiche
import com.empire.server.llm.Personas
import com.empire.server.orchestration.RunManifest
import com.empire.server.orchestration.StageOutcome
import com.empire.server.orchestration.artifacts.ResearchBrief
import com.empire.server.orchestration.readArtifact
import com.empire.server.storage.NicheRepository
import com.empire.server.storage.RunRepository
import com.empire.server.testutil.FakeLlmClient
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ResearchStageTest {
    private fun newRepo(): RunRepository = RunRepository(Files.createTempDirectory("empire-test").toFile())
    private fun newNicheRepo(): NicheRepository = NicheRepository(Files.createTempDirectory("empire-test").toFile())

    private val fallbackNiche = SelectedNiche(
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

    private fun isOriginationPrompt(system: String, user: String) =
        system == Personas.MARKET_TREND_ANALYST && "Identify one promising" in user

    @Test
    fun `falls back to a default niche when the origination response is unparseable`() = runBlocking {
        val repo = newRepo()
        val runId = "run-1"
        repo.save(RunManifest(runId = runId, createdAt = "now"))
        val nicheRepository = newNicheRepo()

        val llm = FakeLlmClient { system, user ->
            if (isOriginationPrompt(system, user)) "not json at all" else "placeholder"
        }
        val result = ResearchStage(llm, nicheRepository, repo).run(runId, repo.load(runId)!!)

        assertEquals(StageOutcome.Continue, result.outcome)
        val brief = readArtifact(repo, runId, "research-brief.json", ResearchBrief.serializer())
        assertEquals(fallbackNiche, brief?.niche)
        assertEquals(fallbackNiche, repo.load(runId)?.niche)
        assertEquals(listOf(fallbackNiche), nicheRepository.all())
    }

    @Test
    fun `parses a well-formed origination response`() = runBlocking {
        val repo = newRepo()
        val runId = "run-1"
        repo.save(RunManifest(runId = runId, createdAt = "now"))
        val nicheRepository = newNicheRepo()

        val parsedNiche = SelectedNiche(
            niche = "Pet care",
            subNiche = "Senior dog nutrition",
            audience = "Owners of older dogs",
            coreProblem = "Confusing, contradictory diet advice",
            demand = 82.0,
            competition = 30.0,
            speed = 70.0,
            legalRisk = 5.0,
            brandFit = 65.0,
            score = 77.0
        )
        val niceJson = """
            {"Niche":"Pet care","SubNiche":"Senior dog nutrition","Audience":"Owners of older dogs",
             "CoreProblem":"Confusing, contradictory diet advice","Demand":82.0,"Competition":30.0,
             "Speed":70.0,"LegalRisk":5.0,"BrandFit":65.0,"Score":77.0}
        """.trimIndent()

        val llm = FakeLlmClient { system, user ->
            if (isOriginationPrompt(system, user)) niceJson else "placeholder"
        }
        ResearchStage(llm, nicheRepository, repo).run(runId, repo.load(runId)!!)

        val brief = readArtifact(repo, runId, "research-brief.json", ResearchBrief.serializer())
        assertEquals(parsedNiche, brief?.niche)
    }

    @Test
    fun `reuses an already-selected niche instead of originating a new one`() = runBlocking {
        val repo = newRepo()
        val runId = "run-1"
        val existingNiche = SelectedNiche(niche = "Existing", subNiche = "Already chosen")
        repo.save(RunManifest(runId = runId, createdAt = "now", niche = existingNiche))
        val nicheRepository = newNicheRepo()

        val llm = FakeLlmClient { system, user ->
            if (isOriginationPrompt(system, user)) error("should not originate a new niche when one is already selected")
            "placeholder"
        }
        val result = ResearchStage(llm, nicheRepository, repo).run(runId, repo.load(runId)!!)

        assertIs<StageOutcome.Continue>(result.outcome)
        val brief = readArtifact(repo, runId, "research-brief.json", ResearchBrief.serializer())
        assertEquals(existingNiche, brief?.niche)
        assertEquals(emptyList(), nicheRepository.all()) // never (re-)added to the niche repository
    }
}
