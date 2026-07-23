package com.empire.server.orchestration.artifacts

import kotlinx.serialization.Serializable

@Serializable
data class EnterpriseBlueprint(
    val productOutline: String,
    val brandVoiceGuide: String,
    val visualDirection: String,
    val pricing: String,
    val leadMagnetConcept: String,
    val funnelDesign: String,
    val outputFormats: List<String>,
    val platformTargets: List<String>
)

@Serializable
data class FormatPlatformDecision(
    val outputFormats: List<String>,
    val platformTargets: List<String>
)
