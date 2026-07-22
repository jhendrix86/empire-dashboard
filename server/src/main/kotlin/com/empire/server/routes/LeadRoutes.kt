package com.empire.server.routes

import com.empire.server.storage.LeadRepository
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable

@Serializable
private data class AddLeadRequest(
    val email: String,
    val name: String? = null,
    val source: String? = null
)

fun Route.leadRoutes(repository: LeadRepository) {
    get("/leads") {
        call.respond(repository.all())
    }
    post("/leads") {
        if (!requireToken(call)) return@post
        val body = call.receive<AddLeadRequest>()
        val lead = repository.add(body.email, body.name, body.source)
        call.respond(lead)
    }
}
