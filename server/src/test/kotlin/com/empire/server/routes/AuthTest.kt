package com.empire.server.routes

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthTest {
    @Test
    fun `loopback-only mode (bindAllInterfaces false) is always authorized, even with no tokens at all`() {
        assertTrue(isAuthorized(bindAllInterfaces = false, providedToken = null, expectedToken = null))
        assertTrue(isAuthorized(bindAllInterfaces = false, providedToken = "wrong", expectedToken = "right"))
    }

    @Test
    fun `LAN mode authorizes a matching token`() {
        assertTrue(isAuthorized(bindAllInterfaces = true, providedToken = "secret", expectedToken = "secret"))
    }

    @Test
    fun `LAN mode rejects a mismatched token`() {
        assertFalse(isAuthorized(bindAllInterfaces = true, providedToken = "wrong", expectedToken = "secret"))
    }

    @Test
    fun `LAN mode rejects a missing request token`() {
        assertFalse(isAuthorized(bindAllInterfaces = true, providedToken = null, expectedToken = "secret"))
    }

    @Test
    fun `LAN mode rejects when no server token is configured`() {
        // Shouldn't happen in practice -- Application.main fails fast on this combination --
        // but the check itself must still fail closed rather than throw or default-allow.
        assertFalse(isAuthorized(bindAllInterfaces = true, providedToken = "anything", expectedToken = null))
    }
}
