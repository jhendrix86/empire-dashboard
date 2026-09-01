package com.empire.server.routes

import com.empire.dashboard.data.Customer
import com.empire.dashboard.data.CustomersResponse
import com.empire.server.storage.CustomerRepository
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

class CustomerRoutesTest {
    private fun newRepo(): CustomerRepository = CustomerRepository(Files.createTempDirectory("empire-test").toFile())

    @Test
    fun `GET customers returns an empty list initially`() = testApplication {
        application {
            install(ContentNegotiation) { json(testJson) }
            routing { customerRoutes(newRepo()) }
        }

        val response = client.get("/customers")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = testJson.decodeFromString(CustomersResponse.serializer(), response.bodyAsText())
        assertEquals(0, body.count)
    }

    @Test
    fun `POST customers adds a customer that GET customers then returns`() = testApplication {
        application {
            install(ContentNegotiation) { json(testJson) }
            routing { customerRoutes(newRepo()) }
        }

        val postResponse = client.post("/customers") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"buyer@example.com","name":"Buyer","product":"ebook","amountPaid":9.99,"source":"gumroad"}""")
        }

        assertEquals(HttpStatusCode.OK, postResponse.status)
        val added = testJson.decodeFromString(Customer.serializer(), postResponse.bodyAsText())
        assertEquals("buyer@example.com", added.email)
        assertEquals(9.99, added.amountPaid)

        val getResponse = client.get("/customers")
        val all = testJson.decodeFromString(CustomersResponse.serializer(), getResponse.bodyAsText())
        assertEquals(1, all.count)
        assertEquals("buyer@example.com", all.customers.single().email)
    }

    @Test
    fun `POST customers defaults optional fields when omitted`() = testApplication {
        application {
            install(ContentNegotiation) { json(testJson) }
            routing { customerRoutes(newRepo()) }
        }

        val response = client.post("/customers") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"minimal@example.com"}""")
        }

        val added = testJson.decodeFromString(Customer.serializer(), response.bodyAsText())
        assertEquals("", added.name)
        assertEquals("", added.product)
        assertEquals(0.0, added.amountPaid)
    }
}
