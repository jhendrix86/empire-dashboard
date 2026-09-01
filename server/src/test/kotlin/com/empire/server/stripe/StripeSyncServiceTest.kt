package com.empire.server.stripe

import com.empire.server.storage.RevenueRepository
import com.empire.server.testutil.FakeNotifier
import com.empire.server.testutil.FakeStripeClient
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StripeSyncServiceTest {
    private fun newRepo(): RevenueRepository = RevenueRepository(Files.createTempDirectory("empire-test").toFile())

    @Test
    fun `records only successful charges and notifies once per new sale`() {
        val repo = newRepo()
        val notifier = FakeNotifier()
        val charges = listOf(
            StripeCharge("ch_1", 19.99, "buyer1@example.com", "ebook", 1_700_000_000, successful = true),
            StripeCharge("ch_2", 9.99, "buyer2@example.com", "guide", 1_700_000_100, successful = false), // refunded/failed
            StripeCharge("ch_3", 29.99, null, null, 1_700_000_200, successful = true)
        )
        val service = StripeSyncService(FakeStripeClient(charges), repo, notifier)

        val recorded = runBlocking { service.sync() }

        assertEquals(2, recorded)
        val all = repo.all()
        assertEquals(2, all.salesCount)
        assertEquals(49.98, all.totalRevenue, 0.0001)
        assertTrue(repo.hasStripeCharge("ch_1"))
        assertTrue(repo.hasStripeCharge("ch_3"))
        assertEquals(false, repo.hasStripeCharge("ch_2"))
        assertEquals(2, notifier.sent.count { it.event == "sale_recorded" })
    }

    @Test
    fun `re-syncing the same charges does not double-count them`() {
        val repo = newRepo()
        val charges = listOf(StripeCharge("ch_1", 19.99, "buyer@example.com", "ebook", 1_700_000_000, successful = true))
        val service = StripeSyncService(FakeStripeClient(charges), repo, FakeNotifier())

        runBlocking {
            service.sync()
            val secondSyncRecorded = service.sync()
            assertEquals(0, secondSyncRecorded)
        }

        assertEquals(1, repo.all().salesCount)
    }
}
