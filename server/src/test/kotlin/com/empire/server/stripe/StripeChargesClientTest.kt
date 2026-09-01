package com.empire.server.stripe

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class StripeChargesClientTest {
    @Test
    fun `returns no charges and does not throw when no secret key is configured`() {
        val charges = runBlocking { StripeChargesClient(secretKey = null).listRecentCharges() }

        assertEquals(emptyList(), charges)
    }
}
