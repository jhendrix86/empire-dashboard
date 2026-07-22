package com.empire.server.orchestration

import com.empire.server.storage.RunRepository
import com.empire.server.storage.appJson
import java.io.File
import kotlinx.serialization.KSerializer

fun <T> writeArtifact(runRepository: RunRepository, runId: String, fileName: String, serializer: KSerializer<T>, value: T) {
    val file = File(runRepository.runDir(runId), fileName)
    file.writeText(appJson.encodeToString(serializer, value))
}

fun <T> readArtifact(runRepository: RunRepository, runId: String, fileName: String, serializer: KSerializer<T>): T? {
    val file = File(runRepository.runDir(runId), fileName)
    if (!file.exists()) return null
    return runCatching { appJson.decodeFromString(serializer, file.readText()) }.getOrNull()
}
