package com.empire.server.routes

import com.empire.server.storage.CustomerRepository
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable

@Serializable
private data class AddCustomerRequest(
    val email: String,
    val name: String? = null,
    val product: String? = null,
    val amountPaid: Double? = null,
    val source: String? = null
)

fun Route.customerRoutes(repository: CustomerRepository) {
    get("/customers") {
        call.respond(repository.all())
    }
    post("/customers") {
        if (!requireToken(call)) return@post
        val body = call.receive<AddCustomerRequest>()
        val customer = repository.add(body.email, body.name, body.product, body.amountPaid, body.source)
        call.respond(customer)
    }
}
