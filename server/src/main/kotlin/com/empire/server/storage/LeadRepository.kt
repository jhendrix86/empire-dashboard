package com.empire.server.storage

import com.empire.dashboard.data.Lead
import com.empire.dashboard.data.LeadsResponse
import com.empire.server.config.AppConfig
import java.io.File
import java.time.Instant

class LeadRepository(dataDir: File = AppConfig.dataDir) {
    private val store = JsonFileStore(
        file = File(dataDir, "leads.json"),
        serializer = LeadsResponse.serializer(),
        default = { LeadsResponse() }
    )

    fun all(): LeadsResponse = store.read()

    fun add(email: String, name: String?, source: String?): Lead {
        val lead = Lead(
            email = email,
            name = name.orEmpty(),
            source = source.orEmpty(),
            dateAdded = Instant.now().toString()
        )
        store.update { current ->
            val updated = current.leads + lead
            current.copy(count = updated.size, leads = updated)
        }
        return lead
    }
}
