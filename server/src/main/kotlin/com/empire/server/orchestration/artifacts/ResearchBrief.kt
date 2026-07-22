package com.empire.server.orchestration.artifacts

import com.empire.dashboard.data.SelectedNiche
import kotlinx.serialization.Serializable

@Serializable
data class ResearchBrief(
    val niche: SelectedNiche,
    val audienceProfile: String,
    val coreProblemDetail: String,
    val competitiveLandscape: String,
    val legalComplianceNotes: String,
    val monetizationAngle: String,
    val recommendedProductFormat: String
)
