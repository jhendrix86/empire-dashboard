package com.empire.server.orchestration.artifacts

import kotlinx.serialization.Serializable

@Serializable
data class DeliverableFile(
    val format: String,
    val fileName: String,
    val description: String
)

@Serializable
data class DeliverableManifest(
    val files: List<DeliverableFile>,
    val leadMagnetFileName: String
)
