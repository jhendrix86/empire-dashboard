package com.empire.server.storage

import com.empire.server.orchestration.RunManifest
import com.empire.server.orchestration.RunStatus
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RunRepositoryTest {
    private fun newRepo(): RunRepository = RunRepository(Files.createTempDirectory("empire-test").toFile())

    @Test
    fun `save and load round-trips a manifest`() {
        val repo = newRepo()
        val manifest = RunManifest(runId = "run-1", createdAt = "2026-01-01T00:00:00Z")

        repo.save(manifest)

        assertEquals(manifest, repo.load("run-1"))
    }

    @Test
    fun `load returns null for an unknown run`() {
        assertNull(newRepo().load("does-not-exist"))
    }

    @Test
    fun `update applies the transform atomically and persists it`() {
        val repo = newRepo()
        repo.save(RunManifest(runId = "run-1", createdAt = "now"))

        val updated = repo.update("run-1") { it.copy(status = RunStatus.DONE) }

        assertEquals(RunStatus.DONE, updated?.status)
        assertEquals(RunStatus.DONE, repo.load("run-1")?.status)
    }

    @Test
    fun `update on an unknown run is a no-op`() {
        val repo = newRepo()

        val updated = repo.update("does-not-exist") { it.copy(status = RunStatus.DONE) }

        assertNull(updated)
    }

    @Test
    fun `current run id round-trips`() {
        val repo = newRepo()

        repo.setCurrentRunId("run-42")

        assertEquals("run-42", repo.currentRunId())
    }

    @Test
    fun `log lines are delivered once each and the cursor advances`() {
        val repo = newRepo()
        repo.appendLog("run-1", "line one")
        repo.appendLog("run-1", "line two")

        val (lines, cursor) = repo.newLogLinesSince("run-1", 0)
        assertEquals(listOf("line one", "line two"), lines)
        assertEquals(2, cursor)

        repo.appendLog("run-1", "line three")
        val (moreLines, nextCursor) = repo.newLogLinesSince("run-1", cursor)
        assertEquals(listOf("line three"), moreLines)
        assertEquals(3, nextCursor)
    }
}
