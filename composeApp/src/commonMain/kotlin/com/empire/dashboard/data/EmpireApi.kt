package com.empire.dashboard.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class EmpireApi(private val baseUrl: String = "http://localhost:8765") {

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    suspend fun fetchStatus(): Result<EmpireStatus> = runCatching {
        client.get("$baseUrl/status").body<EmpireStatus>()
    }

    suspend fun fetchHealth(): Result<Boolean> = runCatching {
        client.get("$baseUrl/health")
        true
    }

    suspend fun fetchNicheScores(): Result<List<SelectedNiche>> = runCatching {
        client.get("$baseUrl/niche-scores").body<List<SelectedNiche>>()
    }

    suspend fun fetchRuns(): Result<List<RunEntry>> = runCatching {
        client.get("$baseUrl/runs").body<List<RunEntry>>()
    }
}
