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

    fun recordSale(amount: Double, email: String?, note: String?): RevenueEntry =
        record(type = "sale", amount = amount, email = email, note = note)

    fun recordRefund(amount: Double, email: String?, note: String?): RevenueEntry =
        record(type = "refund", amount = amount, email = email, note = note)

    private fun record(type: String, amount: Double, email: String?, note: String?): RevenueEntry {
        val entry = RevenueEntry(
            type = type,
            amount = amount,
            email = email.orEmpty(),
            note = note.orEmpty(),
            at = Instant.now().toString()
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
