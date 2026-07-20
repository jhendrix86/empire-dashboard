package com.empire.dashboard.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
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

    suspend fun startPipeline(
        gumroadUrl: String? = null,
        leadMagnetUrl: String? = null,
        yourName: String? = null
    ): Result<RunStartResponse> = runCatching {
        val payload = mutableMapOf<String, String?>(
            "gumroadUrl" to gumroadUrl,
            "leadMagnetUrl" to leadMagnetUrl,
            "yourName" to yourName
        ).filterValues { it != null }
        
        client.post("$baseUrl/run") {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }.body<RunStartResponse>()
    }

    suspend fun getRunProgress(): Result<RunProgress> = runCatching {
        client.get("$baseUrl/run-progress").body<RunProgress>()
    }

    suspend fun addCustomer(
        email: String,
        name: String? = null,
        product: String? = null,
        amountPaid: Double? = null,
        source: String? = null
    ): Result<Customer> = runCatching {
        val payload = mutableMapOf(
            "email" to email,
            "name" to (name ?: ""),
            "product" to (product ?: ""),
            "amountPaid" to (amountPaid ?: 0.0),
            "source" to (source ?: "")
        )
        client.post("$baseUrl/customers") {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }.body<Customer>()
    }

    suspend fun getCustomers(): Result<CustomersResponse> = runCatching {
        client.get("$baseUrl/customers").body<CustomersResponse>()
    }

    suspend fun addLead(
        email: String,
        name: String? = null,
        source: String? = null
    ): Result<Lead> = runCatching {
        val payload = mutableMapOf(
            "email" to email,
            "name" to (name ?: ""),
            "source" to (source ?: "")
        )
        client.post("$baseUrl/leads") {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }.body<Lead>()
    }

    suspend fun getLeads(): Result<LeadsResponse> = runCatching {
        client.get("$baseUrl/leads").body<LeadsResponse>()
    }

    suspend fun getRevenue(): Result<RevenueData> = runCatching {
        client.get("$baseUrl/revenue").body<RevenueData>()
    }

    suspend fun recordSale(
        amount: Double,
        email: String? = null,
        note: String? = null
    ): Result<String> = runCatching {
        val payload = mutableMapOf(
            "amount" to amount,
            "email" to (email ?: ""),
            "note" to (note ?: "")
        )
        client.post("$baseUrl/revenue/sale") {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }.body<String>()
    }

    suspend fun recordRefund(
        amount: Double,
        email: String? = null,
        note: String? = null
    ): Result<String> = runCatching {
        val payload = mutableMapOf(
            "amount" to amount,
            "email" to (email ?: ""),
            "note" to (note ?: "")
        )
        client.post("$baseUrl/revenue/refund") {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }.body<String>()
    }
