package com.empire.server.storage

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LeadRepositoryTest {
    private fun newRepo(): LeadRepository = LeadRepository(Files.createTempDirectory("empire-test").toFile())

    @Test
    fun `starts empty for a fresh data directory`() {
        val repo = newRepo()
        assertEquals(0, repo.all().count)
        assertEquals(emptyList(), repo.all().leads)
    }

    @Test
    fun `add persists the lead, defaults optional fields, and stamps a date`() {
        val repo = newRepo()

        val added = repo.add(email = "lead@example.com", name = null, source = null)

        assertEquals("lead@example.com", added.email)
        assertEquals("", added.name)
        assertEquals("", added.source)
        assertTrue(added.dateAdded.isNotBlank())
        assertEquals(listOf(added), repo.all().leads)
        assertEquals(1, repo.all().count)
    }

    @Test
    fun `count tracks multiple additions in insertion order`() {
        val repo = newRepo()

        repo.add("a@example.com", "A", "landing-page")
        repo.add("b@example.com", "B", "newsletter")

        val all = repo.all()
        assertEquals(2, all.count)
        assertEquals(listOf("a@example.com", "b@example.com"), all.leads.map { it.email })
    }
}
