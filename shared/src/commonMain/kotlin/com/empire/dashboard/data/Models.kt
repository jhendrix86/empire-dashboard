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
    val copiedFiles: List<String> = emptyList(),
    // Admin link to the DRAFT Shopify listing created from this bundle, when Shopify is
    // configured; null if Shopify is unconfigured or listing creation failed.
    val shopifyProductUrl: String? = null,
    // Link to the DRAFT Etsy listing created from this bundle, when Etsy is configured;
    // null if Etsy is unconfigured or listing creation failed.
    val etsyListingUrl: String? = null
)

@Serializable
data class RunEntry(
    val runId: String = "",
    val date: String = "",
    val status: String = "",
    val spentUsd: Double = 0.0,
    val durationSeconds: Double? = null
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
data class RunCancelResponse(
    val cancelled: Boolean = false,
    val error: String? = null
)

@Serializable
data class PipelineStep(
    val name: String = "",
    val status: String = "",
    val detail: String = "",
    val startedAt: String? = null,
    // Set once the step reaches "done"/"error" -- null while pending or running so the
    // client never has to compute a live-updating elapsed time itself.
    val durationSeconds: Double? = null
)

@Serializable
data class RunProgress(
    val status: String = "",
    val progressPct: Double = 0.0,
    val steps: List<PipelineStep> = emptyList(),
    val newLogLines: List<String> = emptyList(),
    val runId: String? = null,
    val error: String? = null,
    val spentUsd: Double = 0.0,
    // Echo this back as the `cursor` query param on the next call to pick up
    // exactly where this response left off -- delivery is then per-client and
    // safe to retry, instead of a single server-side position every poller shares.
    val logCursor: Int = 0
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
    val at: String = "",
    // Set only for a sale pulled in automatically from Stripe; lets the sync dedupe
    // against charges it has already recorded instead of double-counting on re-sync.
    val stripeChargeId: String? = null
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

@Serializable
data class StripeSyncResponse(
    val synced: Int = 0
)
