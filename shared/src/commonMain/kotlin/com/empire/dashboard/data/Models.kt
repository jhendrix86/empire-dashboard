package com.empire.dashboard.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SelectedNiche(
    @SerialName("Niche")       val niche: String = "",
    @SerialName("SubNiche")    val subNiche: String = "",
    @SerialName("Audience")    val audience: String = "",
    @SerialName("CoreProblem") val coreProblem: String = "",
    @SerialName("Demand")      val demand: Double = 0.0,
    @SerialName("Competition") val competition: Double = 0.0,
    @SerialName("Speed")       val speed: Double = 0.0,
    @SerialName("LegalRisk")   val legalRisk: Double = 0.0,
    @SerialName("BrandFit")    val brandFit: Double = 0.0,
    @SerialName("Score")       val score: Double = 0.0
)

@Serializable
data class BundleInfo(
    val productName: String = "",
    val version: String = "",
    val generatedAt: String = "",
    val checksumSha256: String = "",
    val bundleExists: Boolean = false,
    val copiedFiles: List<String> = emptyList()
)

@Serializable
data class RunEntry(
    val runId: String = "",
    val date: String = ""
)

@Serializable
data class EmpireStatus(
    val serverTime: String = "",
    val lastPipelineRun: String? = null,
    val lastRunAt: String? = null,
    val launchStep: String = "unknown",
    val selectedNiche: SelectedNiche? = null,
    val topNiches: List<SelectedNiche> = emptyList(),
    val bundle: BundleInfo? = null,
    val recentPipelines: List<RunEntry> = emptyList(),
    val recentBundles: List<RunEntry> = emptyList()
)

@Serializable
data class RunStartResponse(
    val started: Boolean = false,
    val runId: String = "",
    val error: String? = null
)

@Serializable
data class PipelineStep(
    val name: String = "",
    val status: String = "",
    val detail: String = ""
)

@Serializable
data class RunProgress(
    val status: String = "",
    val progressPct: Double = 0.0,
    val steps: List<PipelineStep> = emptyList(),
    val newLogLines: List<String> = emptyList(),
    val runId: String? = null,
    val error: String? = null
)

@Serializable
data class Customer(
    val email: String = "",
    val name: String = "",
    val product: String = "",
    val amountPaid: Double = 0.0,
    val source: String = "",
    val dateAdded: String = ""
)

@Serializable
data class Lead(
    val email: String = "",
    val name: String = "",
    val source: String = "",
    val dateAdded: String = ""
)

@Serializable
data class RevenueEntry(
    val type: String = "",
    val amount: Double = 0.0,
    val email: String = "",
    val note: String = "",
    val at: String = ""
)

@Serializable
data class RevenueData(
    val totalRevenue: Double = 0.0,
    val totalRefunds: Double = 0.0,
    val salesCount: Int = 0,
    val refundCount: Int = 0,
    val history: List<RevenueEntry> = emptyList()
)

@Serializable
data class CustomersResponse(
    val count: Int = 0,
    val customers: List<Customer> = emptyList()
)

@Serializable
data class LeadsResponse(
    val count: Int = 0,
    val leads: List<Lead> = emptyList()
)

@Serializable
data class RunRequest(
    val gumroadUrl: String? = null,
    val leadMagnetUrl: String? = null,
    val yourName: String? = null
)

@Serializable
data class RevenueMutationRequest(
    val amount: Double = 0.0,
    val email: String? = null,
    val note: String? = null
)
