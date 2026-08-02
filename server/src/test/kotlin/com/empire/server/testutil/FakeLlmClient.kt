package com.empire.server.testutil

import com.empire.server.llm.LlmClient
import com.empire.server.llm.Personas

/** Deterministic [LlmClient] test double: reruns [respond] for every call, no network. */
class FakeLlmClient(
    private val respond: (systemPrompt: String, userPrompt: String) -> String = { _, _ -> "ok" }
) : LlmClient {
    var callCount: Int = 0
        private set

    override suspend fun complete(systemPrompt: String, userPrompt: String): String {
        callCount++
        return respond(systemPrompt, userPrompt)
    }
}

/**
 * A responder that returns valid, parseable output for every persona the real pipeline
 * stages (Research -> Design -> Completion -> Polish -> Shipping) parse as structured
 * data, and plain placeholder text for the free-text ones -- so a full run driven by
 * this responder reaches [com.empire.server.orchestration.RunStatus.DONE] deterministically.
 */
fun happyPathResponder(auditPass: Boolean = true): (String, String) -> String = { system, user ->
    when {
        system == Personas.MARKET_TREND_ANALYST && "Identify one promising" in user ->
            """{"Niche":"Test niche","SubNiche":"Test sub-niche","Audience":"Testers",
               "CoreProblem":"Testing is hard","Demand":80.0,"Competition":20.0,
               "Speed":90.0,"LegalRisk":5.0,"BrandFit":70.0,"Score":85.0}"""
        system == Personas.FORMAT_PLATFORM_DECISION_MAKER ->
            """{"outputFormats": ["ebook"], "platformTargets": ["gumroad"]}"""
        system == Personas.BRAND_VOICE_VISUAL_DIRECTOR -> "VOICE: friendly.\nVISUAL: clean."
        system == Personas.LEAD_MAGNET_FUNNEL_DESIGNER -> "LEAD MAGNET: a checklist.\nFUNNEL: three emails."
        system in setOf(
            Personas.COPYEDITOR_PROOFREADER,
            Personas.BRAND_CONSISTENCY_AUDITOR,
            Personas.LEGAL_COMPLIANCE_REVIEWER,
            Personas.ACCESSIBILITY_EDITORIAL_REVIEWER,
            Personas.GAP_ANALYST
        ) -> """{"pass": $auditPass, "notes": "looks fine"}"""
        else -> "placeholder response text"
    }
}
