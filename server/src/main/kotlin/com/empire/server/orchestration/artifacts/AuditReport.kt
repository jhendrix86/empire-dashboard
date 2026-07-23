package com.empire.server.orchestration.artifacts

import kotlinx.serialization.Serializable

@Serializable
data class AuditCheck(
    val name: String,
    val pass: Boolean,
    val notes: String
)

@Serializable
data class AuditReport(
    val pass: Boolean,
    val checks: List<AuditCheck>,
    val gapAnalysis: String,
    val retryTarget: String? = null // "design" | "completion" | null
)
