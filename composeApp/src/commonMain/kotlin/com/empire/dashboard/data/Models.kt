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
