package com.empire.server.stripe

import com.empire.server.notify.Notifier
import com.empire.server.storage.RevenueRepository

/** Pulls new successful Stripe charges into the revenue ledger, deduping against ones
 *  already recorded and firing the same sale_recorded notification a manual sale does. */
class StripeSyncService(
    private val stripeClient: StripeClient,
    private val revenueRepository: RevenueRepository,
    private val notifier: Notifier
) {
    suspend fun sync(): Int {
        var recorded = 0
        for (charge in stripeClient.listRecentCharges()) {
            if (!charge.successful) continue
            if (revenueRepository.hasStripeCharge(charge.id)) continue

            val entry = revenueRepository.recordSale(
                amount = charge.amountUsd,
                email = charge.email,
                note = charge.description ?: "Stripe charge ${charge.id}",
                stripeChargeId = charge.id
            )
            val who = entry.email.takeIf { it.isNotBlank() }?.let { " from $it" }.orEmpty()
            notifier.notify("sale_recorded", "💰 Sale recorded: \$%.2f%s (via Stripe)".format(entry.amount, who))
            recorded++
        }
        return recorded
    }
}
