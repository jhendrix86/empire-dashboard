package com.empire.server.testutil

import kotlinx.serialization.json.Json

/** Mirrors the ContentNegotiation config Application.module() installs, for route tests. */
val testJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}
