package com.empire.server.orchestration

import com.empire.dashboard.data.BundleInfo
import com.empire.dashboard.data.PipelineStep
import com.empire.dashboard.data.SelectedNiche
import kotlinx.serialization.Serializable

enum class Stage(val slug: String) {
    RESEARCH("research"),
    PRODUCT_DESIGN("product-design"),
    PRODUCT_COMPLETION("product-completion"),
    POLISH_AUDIT("polish-audit"),
    SHIPPING("shipping");

    fun next(): Stage? = Stage.entries.getOrNull(ordinal + 1)
}

/** Client-facing run status: what ProgressScreen's poll loop checks to stop polling. */
object RunStatus {
    const val RUNNING = "running"
    const val DONE = "done"
    const val ERROR = "error"
}

/** Server-internal status: distinguishes a genuine failure from the retry-bound safety valve. */
object InternalStatus {
    const val RUNNING = "running"
    const val DONE = "done"
    const val NEEDS_ATTENTION = "needs_attention"
}

@Serializable
data class RunManifest(
    val runId: String,
    val createdAt: String,
    val status: String = RunStatus.RUNNING,
    val internalStatus: String = InternalStatus.RUNNING,
    val currentStage: String = Stage.RESEARCH.slug,
    val retryCount: Int = 0,
    val steps: List<PipelineStep> = Stage.entries.map { PipelineStep(name = it.slug, status = "pending", detail = "") },
    val niche: SelectedNiche? = null,
    val bundle: BundleInfo? = null,
    val error: String? = null
) {
    val progressPct: Double
        get() = steps.count { it.status == "done" } * 100.0 / steps.size
}
