package com.empire.server.routes

import com.empire.dashboard.data.RevenueMutationRequest
import com.empire.server.storage.RevenueRepository
import io.ktor.http.ContentType
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.revenueRoutes(repository: RevenueRepository) {
    get("/revenue") {
        call.respond(repository.all())
    }
    post("/revenue/sale") {
        val body = call.receive<RevenueMutationRequest>()
        repository.recordSale(body.amount, body.email, body.note)
        call.respondText(text = "\"ok\"", contentType = ContentType.Application.Json)
    }
    post("/revenue/refund") {
        val body = call.receive<RevenueMutationRequest>()
        repository.recordRefund(body.amount, body.email, body.note)
        call.respondText(text = "\"ok\"", contentType = ContentType.Application.Json)
    }
}
