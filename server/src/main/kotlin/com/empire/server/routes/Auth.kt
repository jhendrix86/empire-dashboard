package com.empire.server.routes

import com.empire.server.config.AppConfig
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.header
import io.ktor.server.response.respondText
import java.security.MessageDigest

/**
 * Gate for mutating routes. A no-op unless the server has opted into LAN exposure
 * (AppConfig.bindAllInterfaces) -- loopback-only access is already safe by network
 * isolation. Returns true (and does nothing) when authorized; otherwise responds
 * 401 and returns false. Callers must `return@post` immediately when this is false.
 */
suspend fun requireToken(call: ApplicationCall): Boolean {
    if (isAuthorized(AppConfig.bindAllInterfaces, call.request.header("X-Empire-Token"), AppConfig.authToken)) return true

    call.respondText("unauthorized", status = HttpStatusCode.Unauthorized)
    return false
}

/** The actual authorization decision, pulled out of [requireToken] so it's testable without
 *  a Ktor test host or reaching into AppConfig's process-wide, env-var-backed singleton state. */
fun isAuthorized(bindAllInterfaces: Boolean, providedToken: String?, expectedToken: String?): Boolean {
    if (!bindAllInterfaces) return true
    return providedToken != null && expectedToken != null && constantTimeEquals(providedToken, expectedToken)
}

/** Avoids leaking how many leading characters matched via response-time differences. */
private fun constantTimeEquals(a: String, b: String): Boolean =
    MessageDigest.isEqual(a.toByteArray(), b.toByteArray())
