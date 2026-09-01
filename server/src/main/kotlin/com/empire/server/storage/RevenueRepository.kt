package com.empire.server.storage

import com.empire.dashboard.data.RevenueData
import com.empire.dashboard.data.RevenueEntry
import com.empire.server.config.AppConfig
import java.io.File
import java.time.Instant

class RevenueRepository(dataDir: File = AppConfig.dataDir) {
    private val store = JsonFileStore(
        file = File(dataDir, "revenue.json"),
        serializer = RevenueData.serializer(),
        default = { RevenueData() }
    )

    fun all(): RevenueData = store.read()

    fun recordSale(amount: Double, email: String?, note: String?, stripeChargeId: String? = null): RevenueEntry =
        record(type = "sale", amount = amount, email = email, note = note, stripeChargeId = stripeChargeId)

    fun recordRefund(amount: Double, email: String?, note: String?): RevenueEntry =
        record(type = "refund", amount = amount, email = email, note = note, stripeChargeId = null)

    /** Lets the Stripe sync skip a charge it has already recorded instead of double-counting it. */
    fun hasStripeCharge(stripeChargeId: String): Boolean =
        store.read().history.any { it.stripeChargeId == stripeChargeId }

    private fun record(type: String, amount: Double, email: String?, note: String?, stripeChargeId: String?): RevenueEntry {
        val entry = RevenueEntry(
            type = type,
            amount = amount,
            email = email.orEmpty(),
            note = note.orEmpty(),
            at = Instant.now().toString(),
            stripeChargeId = stripeChargeId
        )
        store.update { current ->
            val history = current.history + entry
            if (type == "sale") {
                current.copy(
                    totalRevenue = current.totalRevenue + amount,
                    salesCount = current.salesCount + 1,
                    history = history
                )
            } else {
                current.copy(
                    totalRefunds = current.totalRefunds + amount,
                    refundCount = current.refundCount + 1,
                    history = history
                )
            }
        }
        return entry
    }
}
