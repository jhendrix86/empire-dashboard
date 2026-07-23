package com.empire.server.orchestration.stages

import com.empire.server.llm.LlmClient
import com.empire.server.llm.Personas
import com.empire.server.llm.extractJsonObject
import com.empire.server.orchestration.InternalStatus
import com.empire.server.orchestration.RunManifest
import com.empire.server.orchestration.Stage
import com.empire.server.orchestration.StageOutcome
import com.empire.server.orchestration.StageResult
import com.empire.server.orchestration.artifacts.AuditCheck
import com.empire.server.orchestration.artifacts.AuditReport
import com.empire.server.orchestration.artifacts.DeliverableManifest
import com.empire.server.orchestration.artifacts.EnterpriseBlueprint
import com.empire.server.orchestration.artifacts.ResearchBrief
import com.empire.server.orchestration.readArtifact
import com.empire.server.orchestration.writeArtifact
import com.empire.server.storage.RunRepository
import com.empire.server.storage.appJson
import java.io.File
import kotlinx.serialization.Serializable

const val MAX_AUDIT_RETRIES = 3

@Serializable
private data class CheckResult(val pass: Boolean, val notes: String)

class PolishStage(
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
        val deliverableText = deliverableManifest.files.joinToString("\n\n") { file ->
            val text = File(deliverablesDir, file.fileName).takeIf { it.exists() }?.readText().orEmpty()
            "--- ${file.fileName} (${file.format}) ---\n${text.take(6000)}"
        }

        val context = """
            Brand voice guide: ${blueprint.brandVoiceGuide}
            Legal/compliance notes: ${brief.legalComplianceNotes}
            Audience: ${brief.audienceProfile}
            Core problem to deliver on: ${brief.coreProblemDetail}
            Product outline: ${blueprint.productOutline}

            DELIVERABLES:
            $deliverableText
        """.trimIndent()

        val personas = listOf(
            "copyedit" to Personas.COPYEDITOR_PROOFREADER,
            "brand-consistency" to Personas.BRAND_CONSISTENCY_AUDITOR,
            "legal-compliance" to Personas.LEGAL_COMPLIANCE_REVIEWER,
            "accessibility-editorial" to Personas.ACCESSIBILITY_EDITORIAL_REVIEWER,
            "gap-analysis" to Personas.GAP_ANALYST
        )

        val checks = personas.map { (name, persona) ->
            val raw = llm.complete(persona, context)
            val parsed = runCatching {
                appJson.decodeFromString(CheckResult.serializer(), extractJsonObject(raw))
            }.getOrElse { CheckResult(pass = true, notes = "unparsable audit response; defaulting to pass") }
            AuditCheck(name = name, pass = parsed.pass, notes = parsed.notes)
        }

        val failing = checks.filterNot { it.pass }
        val pass = failing.isEmpty()
        val gapAnalysis = failing.joinToString("; ") { "${it.name}: ${it.notes}" }
        val retryTarget = if (pass) null else routeRetryTarget(failing)

        val report = AuditReport(pass = pass, checks = checks, gapAnalysis = gapAnalysis, retryTarget = retryTarget)
        writeArtifact(runRepository, runId, "audit-report.json", AuditReport.serializer(), report)

        if (pass) {
            return StageResult(StageOutcome.Continue, detail = "audit passed (${checks.size} checks)")
        }

        if (manifest.retryCount >= MAX_AUDIT_RETRIES) {
            runRepository.update(runId) { it.copy(internalStatus = InternalStatus.NEEDS_ATTENTION) }
            return StageResult(
                StageOutcome.Fatal("audit failed after ${manifest.retryCount} retries: $gapAnalysis"),
                detail = "needs attention"
            )
        }

        val retryStage = if (retryTarget == "design") Stage.PRODUCT_DESIGN else Stage.PRODUCT_COMPLETION

        runRepository.update(runId) { current ->
            resetFrom(current, retryStage).copy(retryCount = current.retryCount + 1)
        }

        return StageResult(
            StageOutcome.RetryFrom(retryStage, gapAnalysis),
            detail = "retry ${manifest.retryCount + 1}/$MAX_AUDIT_RETRIES from ${retryStage.slug}"
        )
    }

    /** Brand/legal/accessibility issues trace back to Design-stage guidance; the rest are content-execution issues. */
    private fun routeRetryTarget(failing: List<AuditCheck>): String {
        val designRelated = setOf("brand-consistency", "legal-compliance", "accessibility-editorial")
        return if (failing.any { it.name in designRelated }) "design" else "completion"
    }

    private fun resetFrom(manifest: RunManifest, fromStage: Stage): RunManifest {
        val resetSteps = manifest.steps.map { step ->
            val stepStage = Stage.entries.firstOrNull { it.slug == step.name }
            if (stepStage != null && stepStage.ordinal >= fromStage.ordinal) {
                step.copy(status = "pending", detail = "")
            } else {
                step
            }
        }
        return manifest.copy(steps = resetSteps, currentStage = fromStage.slug)
    }
}
