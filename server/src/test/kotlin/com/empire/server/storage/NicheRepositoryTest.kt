package com.empire.server.storage

import com.empire.dashboard.data.SelectedNiche
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class NicheRepositoryTest {
    private fun newRepo(): NicheRepository = NicheRepository(Files.createTempDirectory("empire-test").toFile())

    @Test
    fun `added niches are persisted and returned in insertion order`() {
        val repo = newRepo()
        val first = SelectedNiche(niche = "A", subNiche = "A1")
        val second = SelectedNiche(niche = "B", subNiche = "B1")

        repo.add(first)
        repo.add(second)

        assertEquals(listOf(first, second), repo.all())
    }

    @Test
    fun `starts empty for a fresh data directory`() {
        assertEquals(emptyList(), newRepo().all())
    }
}
