package com.empire.server.storage

import com.empire.dashboard.data.SelectedNiche
import com.empire.server.config.AppConfig
import java.io.File
import kotlinx.serialization.builtins.ListSerializer

class NicheRepository(dataDir: File = AppConfig.dataDir) {
    private val store = JsonFileStore(
        file = File(dataDir, "niches.json"),
        serializer = ListSerializer(SelectedNiche.serializer()),
        default = { emptyList() }
    )

    fun all(): List<SelectedNiche> = store.read()

    fun add(niche: SelectedNiche) {
        store.update { current -> current + niche }
    }
}
