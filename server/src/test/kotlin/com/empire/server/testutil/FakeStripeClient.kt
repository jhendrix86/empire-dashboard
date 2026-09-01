package com.empire.server.testutil

import com.empire.server.stripe.StripeCharge
import com.empire.server.stripe.StripeClient

/** Returns a fixed list of charges instead of calling Stripe. */
class FakeStripeClient(private val charges: List<StripeCharge> = emptyList()) : StripeClient {
    override suspend fun listRecentCharges(): List<StripeCharge> = charges
}
