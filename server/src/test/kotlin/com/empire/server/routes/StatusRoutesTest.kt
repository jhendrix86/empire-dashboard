package com.empire.server.routes

import com.empire.dashboard.data.EmpireStatus
import com.empire.dashboard.data.RunEntry
import com.empire.dashboard.data.SelectedNiche
import com.empire.server.orchestration.RunManifest
import com.empire.server.orchestration.RunStatus
import com.empire.server.storage.NicheRepository
import com.empire.server.storage.RunRepository
import com.empire.server.testutil.testJson
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import java.nio.file.Files
import kotlinx.serialization.builtins.ListSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StatusRoutesTest {
    private fun newRunRepo(): RunRepository = RunRepository(Files.createTempDirectory("empire-test").toFile())
    private fun newNicheRepo(): NicheRepository = NicheRepository(Files.createTempDirectory("empire-test").toFile())

    @Test
    fun `GET status reports unknown launch step when no run has ever happened`() = testApplication {
        application {
            install(ContentNegotiation) { json(testJson) }
            routing { statusRoutes(newRunRepo(), newNicheRepo()) }
        }

        val response = client.get("/status")

        assertEquals(HttpStatusCode.OK, response.status)
        val status = testJson.decodeFromString(EmpireStatus.serializer(), response.bodyAsText())
        assertEquals("unknown", status.launchStep)
        assertNull(status.lastPipelineRun)
        assertEquals(emptyList(), status.recentPipelines)
    }

    @Test
    fun `GET status reflects the latest run's niche, status, and spend`() = testApplication {
        val runRepo = newRunRepo()
        val niche = SelectedNiche(niche = "Pet care", subNiche = "Senior dog nutrition", score = 80.0)
        runRepo.save(
            RunManifest(runId = "run-1", createdAt = "2026-01-01T00:00:00Z", status = RunStatus.DONE, niche = niche, spentUsd = 1.23)
        )
        application {
            install(ContentNegotiation) { json(testJson) }
            routing { statusRoutes(runRepo, newNicheRepo()) }
        }

        val response = client.get("/status")

        val status = testJson.decodeFromString(EmpireStatus.serializer(), response.bodyAsText())
        assertEquals("run-1", status.lastPipelineRun)
        assertEquals("executed", status.launchStep)
        assertEquals(niche, status.selectedNiche)
        assertEquals(1, status.recentPipelines.size)
        assertEquals(RunEntry(runId = "run-1", date = "2026-01-01T00:00:00Z", status = RunStatus.DONE, spentUsd = 1.23), status.recentPipelines.single())
    }

    @Test
    fun `GET niche-scores returns niches sorted by score descending`() = testApplication {
        val nicheRepo = newNicheRepo()
        nicheRepo.add(SelectedNiche(niche = "Low", subNiche = "Low", score = 20.0))
        nicheRepo.add(SelectedNiche(niche = "High", subNiche = "High", score = 90.0))
        nicheRepo.add(SelectedNiche(niche = "Mid", subNiche = "Mid", score = 50.0))
        application {
            install(ContentNegotiation) { json(testJson) }
            routing { statusRoutes(newRunRepo(), nicheRepo) }
        }

        val response = client.get("/niche-scores")

        val niches = testJson.decodeFromString(ListSerializer(SelectedNiche.serializer()), response.bodyAsText())
        assertEquals(listOf("High", "Mid", "Low"), niches.map { it.niche })
    }

    @Test
    fun `GET runs returns run entries with status and spend`() = testApplication {
        val runRepo = newRunRepo()
        runRepo.save(RunManifest(runId = "run-1", createdAt = "now", status = RunStatus.ERROR, spentUsd = 0.5))
        application {
            install(ContentNegotiation) { json(testJson) }
            routing { statusRoutes(runRepo, newNicheRepo()) }
        }

        val response = client.get("/runs")

        val runs = testJson.decodeFromString(ListSerializer(RunEntry.serializer()), response.bodyAsText())
        assertEquals(RunStatus.ERROR, runs.single().status)
        assertEquals(0.5, runs.single().spentUsd)
    }
}
