package com.empire.server.orchestration.stages

import com.empire.server.llm.LlmClient
import com.empire.server.llm.Personas
import com.empire.server.orchestration.RunManifest
import com.empire.server.orchestration.StageOutcome
import com.empire.server.orchestration.StageResult
import com.empire.server.orchestration.artifacts.DeliverableManifest
import com.empire.server.orchestration.artifacts.EnterpriseBlueprint
import com.empire.server.orchestration.artifacts.ResearchBrief
import com.empire.server.orchestration.readArtifact
import com.empire.server.packaging.BundlePackager
import com.empire.server.storage.RunRepository
import java.io.File

class ShippingStage(
    private val llm: LlmClient,
    private val runRepository: RunRepository
) {
    suspend fun run(runId: String, manifest: RunManifest): StageResult {
        val brief = readArtifact(runRepository, runId, "research-brief.json", ResearchBrief.serializer())
            ?: return StageResult(StageOutcome.Fatal("research brief missing for $runId"))
        val blueprint = readArtifact(runRepository, runId, "blueprint.json", EnterpriseBlueprint.serializer())
            ?: return StageResult(StageOutcome.Fatal("blueprint missing for $runId"))
        val deliverableManifest = readArtifact(runRepository, runId, "deliverable-manifest.json", DeliverableManifest.serializer())
            ?: return StageResult(StageOutcome.Fatal("deliverable manifest missing for $runId"))

        val deliverablesDir = File(runRepository.runDir(runId), "deliverables")
        val bundleDir = File(runRepository.runDir(runId), "bundle").apply { mkdirs() }

        val instructions = llm.complete(
            Personas.SHIPPING_LAUNCH_COORDINATOR,
            """
                Product: ${brief.niche.subNiche} (${brief.niche.niche})
                Pricing: ${blueprint.pricing}
                Platforms to list on: ${blueprint.platformTargets.joinToString(", ")}
                Funnel design: ${blueprint.funnelDesign}
                Files included: ${deliverableManifest.files.joinToString(", ") { it.fileName }}, ${deliverableManifest.leadMagnetFileName}

                Write the complete launch instructions for the human operator now.
            """.trimIndent()
        )

        val instructionsFile = File(bundleDir, "instructions.md")
        instructionsFile.writeText(instructions)

        val sourceFiles = deliverableManifest.files.map { File(deliverablesDir, it.fileName) } +
            File(deliverablesDir, deliverableManifest.leadMagnetFileName) +
            instructionsFile

        val productName = brief.niche.subNiche.ifBlank { brief.niche.niche }.ifBlank { "product" }
        val bundleInfo = BundlePackager.createBundle(
            bundleDir = bundleDir,
            productName = productName,
            version = "1.0.0",
            sourceFiles = sourceFiles
        )

        runRepository.update(runId) { it.copy(bundle = bundleInfo) }

        return StageResult(StageOutcome.Continue, detail = "${bundleInfo.copiedFiles.size} files bundled")
    }
}
