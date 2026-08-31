package com.empire.server.routes

import com.empire.dashboard.data.RevenueData
import com.empire.server.storage.RevenueRepository
import com.empire.server.testutil.FakeNotifier
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

    @Test
    fun `GET revenue starts at zero`() = testApplication {
        application {
            install(ContentNegotiation) { json(testJson) }
            routing { revenueRoutes(newRepo(), FakeNotifier()) }
        }

        val response = client.get("/revenue")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = testJson.decodeFromString(RevenueData.serializer(), response.bodyAsText())
        assertEquals(0.0, body.totalRevenue)
        assertEquals(0, body.salesCount)
    }

    @Test
    fun `POST revenue-sale records the sale and fires a sale_recorded notification`() = testApplication {
        val notifier = FakeNotifier()
        application {
            install(ContentNegotiation) { json(testJson) }
            routing { revenueRoutes(newRepo(), notifier) }
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
        val notifier = FakeNotifier()
        application {
            install(ContentNegotiation) { json(testJson) }
            routing { revenueRoutes(newRepo(), notifier) }
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
}
