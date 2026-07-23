package com.empire.server.storage

import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

val appJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    prettyPrint = true
}

/** Generic atomic read/write for a single JSON-serializable value backed by a file on disk. */
class JsonFileStore<T>(
    private val file: File,
    private val serializer: KSerializer<T>,
    private val default: () -> T
) {
    private val lock = ReentrantLock()

    fun read(): T = lock.withLock {
        if (!file.exists()) {
            default()
        } else {
            try {
                appJson.decodeFromString(serializer, file.readText())
            } catch (e: Exception) {
                default()
            }
        }
    }

    fun write(value: T): Unit = lock.withLock {
        writeTextAtomically(file, appJson.encodeToString(serializer, value))
    }

    fun update(transform: (T) -> T): T = lock.withLock {
        val next = transform(read())
        write(next)
        next
    }
}
