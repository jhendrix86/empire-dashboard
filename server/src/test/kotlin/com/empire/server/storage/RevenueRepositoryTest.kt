package com.empire.server.storage

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RevenueRepositoryTest {
    private fun newRepo(): RevenueRepository = RevenueRepository(Files.createTempDirectory("empire-test").toFile())

    @Test
    fun `starts at zero for a fresh data directory`() {
        val all = newRepo().all()
        assertEquals(0.0, all.totalRevenue)
        assertEquals(0.0, all.totalRefunds)
        assertEquals(0, all.salesCount)
        assertEquals(0, all.refundCount)
        assertEquals(emptyList(), all.history)
    }

    @Test
    fun `recordSale accumulates revenue and sales count without touching refunds`() {
        val repo = newRepo()

        repo.recordSale(amount = 19.99, email = "buyer@example.com", note = "first sale")
        repo.recordSale(amount = 9.99, email = null, note = null)

        val all = repo.all()
        assertEquals(29.98, all.totalRevenue, 0.0001)
        assertEquals(2, all.salesCount)
        assertEquals(0.0, all.totalRefunds)
        assertEquals(0, all.refundCount)
        assertEquals(listOf("sale", "sale"), all.history.map { it.type })
    }

    @Test
    fun `recordRefund accumulates refunds and refund count without touching revenue`() {
        val repo = newRepo()

        repo.recordSale(amount = 19.99, email = "buyer@example.com", note = null)
        repo.recordRefund(amount = 19.99, email = "buyer@example.com", note = "requested refund")

        val all = repo.all()
        assertEquals(19.99, all.totalRevenue, 0.0001)
        assertEquals(1, all.salesCount)
        assertEquals(19.99, all.totalRefunds, 0.0001)
        assertEquals(1, all.refundCount)
        assertEquals(listOf("sale", "refund"), all.history.map { it.type })
    }

    @Test
    fun `defaults a missing email and note to empty strings rather than null`() {
        val repo = newRepo()

        repo.recordSale(amount = 5.0, email = null, note = null)

        val entry = repo.all().history.single()
        assertEquals("", entry.email)
        assertEquals("", entry.note)
    }

    @Test
    fun `hasStripeCharge tracks sales recorded with a stripe charge id`() {
        val repo = newRepo()

        assertFalse(repo.hasStripeCharge("ch_123"))
        repo.recordSale(amount = 19.99, email = "buyer@example.com", note = null, stripeChargeId = "ch_123")

        assertTrue(repo.hasStripeCharge("ch_123"))
        assertFalse(repo.hasStripeCharge("ch_456"))
    }

    @Test
    fun `a manually recorded sale has no stripe charge id`() {
        val repo = newRepo()

        repo.recordSale(amount = 19.99, email = "buyer@example.com", note = null)

        assertEquals(null, repo.all().history.single().stripeChargeId)
    }
}
