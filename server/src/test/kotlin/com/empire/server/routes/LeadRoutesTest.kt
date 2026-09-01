package com.empire.server.routes

import com.empire.dashboard.data.Lead
import com.empire.dashboard.data.LeadsResponse
import com.empire.server.storage.LeadRepository
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

class LeadRoutesTest {
    private fun newRepo(): LeadRepository = LeadRepository(Files.createTempDirectory("empire-test").toFile())

    @Test
    fun `GET leads returns an empty list initially`() = testApplication {
        application {
            install(ContentNegotiation) { json(testJson) }
            routing { leadRoutes(newRepo()) }
        }

        val response = client.get("/leads")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = testJson.decodeFromString(LeadsResponse.serializer(), response.bodyAsText())
        assertEquals(0, body.count)
    }

    @Test
    fun `POST leads adds a lead that GET leads then returns`() = testApplication {
        application {
            install(ContentNegotiation) { json(testJson) }
            routing { leadRoutes(newRepo()) }
        }

        val postResponse = client.post("/leads") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"lead@example.com","name":"Lead","source":"landing-page"}""")
        }

        assertEquals(HttpStatusCode.OK, postResponse.status)
        val added = testJson.decodeFromString(Lead.serializer(), postResponse.bodyAsText())
        assertEquals("lead@example.com", added.email)

        val getResponse = client.get("/leads")
        val all = testJson.decodeFromString(LeadsResponse.serializer(), getResponse.bodyAsText())
        assertEquals(1, all.count)
        assertEquals("lead@example.com", all.leads.single().email)
    }
}
