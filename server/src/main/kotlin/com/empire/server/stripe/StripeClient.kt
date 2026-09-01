package com.empire.server.stripe

/** One successful-or-not charge as reported by Stripe. */
data class StripeCharge(
    val id: String,
    val amountUsd: Double,
    val email: String?,
    val description: String?,
    val createdUnixSeconds: Long,
    val successful: Boolean
)

interface StripeClient {
    /**
     * The most recent charges (newest first), up to Stripe's page size. No pagination cursor
     * is kept between calls -- fine for a periodic sync as long as fewer than ~100 charges
     * land between syncs; callers dedupe against what they've already recorded.
     */
    suspend fun listRecentCharges(): List<StripeCharge>
}
