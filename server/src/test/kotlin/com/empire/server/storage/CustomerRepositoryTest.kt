package com.empire.server.storage

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CustomerRepositoryTest {
    private fun newRepo(): CustomerRepository = CustomerRepository(Files.createTempDirectory("empire-test").toFile())

    @Test
    fun `starts empty for a fresh data directory`() {
        val repo = newRepo()
        assertEquals(0, repo.all().count)
        assertEquals(emptyList(), repo.all().customers)
    }

    @Test
    fun `add persists the customer, defaults optional fields, and stamps a date`() {
        val repo = newRepo()

        val added = repo.add(email = "buyer@example.com", name = null, product = null, amountPaid = null, source = null)

        assertEquals("buyer@example.com", added.email)
        assertEquals("", added.name)
        assertEquals(0.0, added.amountPaid)
        assertTrue(added.dateAdded.isNotBlank())
        assertEquals(listOf(added), repo.all().customers)
        assertEquals(1, repo.all().count)
    }

    @Test
    fun `count tracks multiple additions in insertion order`() {
        val repo = newRepo()

        repo.add("a@example.com", "A", "ebook", 9.99, "gumroad")
        repo.add("b@example.com", "B", "guide", 19.99, "etsy")

        val all = repo.all()
        assertEquals(2, all.count)
        assertEquals(listOf("a@example.com", "b@example.com"), all.customers.map { it.email })
    }
}
