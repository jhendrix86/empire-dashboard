package com.empire.server.orchestration.stages

import com.empire.server.llm.AnthropicClient
import com.empire.server.llm.Personas
import com.empire.server.orchestration.RunManifest
import com.empire.server.orchestration.StageOutcome
import com.empire.server.orchestration.StageResult
import com.empire.server.orchestration.artifacts.DeliverableFile
import com.empire.server.orchestration.artifacts.DeliverableManifest
import com.empire.server.orchestration.artifacts.EnterpriseBlueprint
import com.empire.server.orchestration.artifacts.ResearchBrief
import com.empire.server.orchestration.readArtifact
import com.empire.server.orchestration.writeArtifact
import com.empire.server.storage.RunRepository
import java.io.File

class CompletionStage(
    private val llm: AnthropicClient,
    private val runRepository: RunRepository
) {
    suspend fun run(runId: String, manifest: RunManifest): StageResult {
        val brief = readArtifact(runRepository, runId, "research-brief.json", ResearchBrief.serializer())
            ?: return StageResult(StageOutcome.Fatal("research brief missing for $runId"))
        val blueprint = readArtifact(runRepository, runId, "blueprint.json", EnterpriseBlueprint.serializer())
            ?: return StageResult(StageOutcome.Fatal("blueprint missing for $runId"))

        val deliverablesDir = File(runRepository.runDir(runId), "deliverables").apply { mkdirs() }
        val context = """
            Product outline: ${blueprint.productOutline}
            Brand voice: ${blueprint.brandVoiceGuide}
            Audience: ${brief.audienceProfile}
            Core problem: ${brief.coreProblemDetail}
        """.trimIndent()

        val files = mutableListOf<DeliverableFile>()

        blueprint.outputFormats.forEach { format ->
            val content = llm.complete(
                personaFor(format),
                "$context\n\nGenerate the complete product content for the \"$format\" format now."
            )
            val fileName = "product-${slug(format)}.${extensionFor(format)}"
            File(deliverablesDir, fileName).writeText(content)
            files += DeliverableFile(format = format, fileName = fileName, description = "Main product deliverable ($format)")
        }

        blueprint.platformTargets.forEach { platform ->
            val listingCopy = llm.complete(
                Personas.MARKETPLACE_LISTING_COPYWRITER,
                "$context\n\nWrite complete $platform listing copy (title, description, tags/price suggestion) for this product."
            )
            val fileName = "listing-${slug(platform)}.md"
            File(deliverablesDir, fileName).writeText(listingCopy)
            files += DeliverableFile(format = "listing", fileName = fileName, description = "$platform marketplace listing copy")
        }

        val leadMagnetContent = llm.complete(
            Personas.LEAD_MAGNET_WRITER,
            "$context\n\nLead magnet concept: ${blueprint.leadMagnetConcept}\n\nWrite the complete lead magnet content now."
        )
        val leadMagnetFileName = "lead-magnet.md"
        File(deliverablesDir, leadMagnetFileName).writeText(leadMagnetContent)

        val deliverableManifest = DeliverableManifest(files = files, leadMagnetFileName = leadMagnetFileName)
        writeArtifact(runRepository, runId, "deliverable-manifest.json", DeliverableManifest.serializer(), deliverableManifest)

        return StageResult(StageOutcome.Continue, detail = "${files.size} deliverable file(s) generated")
    }

    private fun personaFor(format: String): String =
        if (format.contains("app", ignoreCase = true) || format.contains("web", ignoreCase = true)) {
            Personas.WEB_APP_SCAFFOLDER
        } else {
            Personas.EBOOK_AUTHOR
        }

    private fun extensionFor(format: String): String =
        if (format.contains("app", ignoreCase = true) || format.contains("web", ignoreCase = true)) "html" else "md"

    private fun slug(text: String): String = text.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
}
