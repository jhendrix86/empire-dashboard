package com.empire.server.routes

import com.empire.dashboard.data.RevenueData
import com.empire.dashboard.data.StripeSyncResponse
import com.empire.server.storage.RevenueRepository
import com.empire.server.stripe.StripeCharge
import com.empire.server.stripe.StripeSyncService
import com.empire.server.testutil.FakeNotifier
import com.empire.server.testutil.FakeStripeClient
import com.empire.server.testutil.testJson
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.install
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RevenueRoutesTest {
    private fun newRepo(): RevenueRepository = RevenueRepository(Files.createTempDirectory("empire-test").toFile())

    private fun noopStripeSync(repo: RevenueRepository, notifier: FakeNotifier = FakeNotifier()): StripeSyncService =
        StripeSyncService(FakeStripeClient(), repo, notifier)

    @Test
    fun `GET revenue starts at zero`() = testApplication {
        val repo = newRepo()
        application {
            install(ContentNegotiation) { json(testJson) }
            routing { revenueRoutes(repo, FakeNotifier(), noopStripeSync(repo)) }
        }

        val response = client.get("/revenue")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = testJson.decodeFromString(RevenueData.serializer(), response.bodyAsText())
        assertEquals(0.0, body.totalRevenue)
        assertEquals(0, body.salesCount)
    }

    @Test
    fun `POST revenue-sale records the sale and fires a sale_recorded notification`() = testApplication {
        val repo = newRepo()
        val notifier = FakeNotifier()
        application {
            install(ContentNegotiation) { json(testJson) }
            routing { revenueRoutes(repo, notifier, noopStripeSync(repo)) }
        }

        val response = client.post("/revenue/sale") {
            contentType(ContentType.Application.Json)
            setBody("""{"amount":19.99,"email":"buyer@example.com","note":"first sale"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val getResponse = client.get("/revenue")
        val body = testJson.decodeFromString(RevenueData.serializer(), getResponse.bodyAsText())
        assertEquals(19.99, body.totalRevenue)
        assertEquals(1, body.salesCount)

        assertEquals(1, notifier.sent.size)
        assertEquals("sale_recorded", notifier.sent.single().event)
        assertTrue(notifier.sent.single().message.contains("19.99"))
    }

    @Test
    fun `POST revenue-refund records the refund and does not notify`() = testApplication {
        val repo = newRepo()
        val notifier = FakeNotifier()
        application {
            install(ContentNegotiation) { json(testJson) }
            routing { revenueRoutes(repo, notifier, noopStripeSync(repo)) }
        }

        val response = client.post("/revenue/refund") {
            contentType(ContentType.Application.Json)
            setBody("""{"amount":19.99,"email":"buyer@example.com","note":"requested refund"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val getResponse = client.get("/revenue")
        val body = testJson.decodeFromString(RevenueData.serializer(), getResponse.bodyAsText())
        assertEquals(19.99, body.totalRefunds)
        assertEquals(1, body.refundCount)

        assertTrue(notifier.sent.isEmpty())
    }

    @Test
    fun `POST revenue-sync-stripe pulls new charges into the ledger`() = testApplication {
        val repo = newRepo()
        val charges = listOf(StripeCharge("ch_1", 19.99, "buyer@example.com", "ebook", 1_700_000_000, successful = true))
        val stripeSync = StripeSyncService(FakeStripeClient(charges), repo, FakeNotifier())
        application {
            install(ContentNegotiation) { json(testJson) }
            routing { revenueRoutes(repo, FakeNotifier(), stripeSync) }
        }

        val response = client.post("/revenue/sync-stripe")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = testJson.decodeFromString(StripeSyncResponse.serializer(), response.bodyAsText())
        assertEquals(1, body.synced)
        assertEquals(1, repo.all().salesCount)
    }
}
