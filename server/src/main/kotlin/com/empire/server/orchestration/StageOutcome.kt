package com.empire.server.orchestration

/**
 * Result of running a single stage. RetryFrom/Fatal exist now so the Polish/Audit
 * self-correction loop (added later) only needs to change what runStage returns
 * for Stage.POLISH_AUDIT, not the orchestration loop itself.
 */
sealed class StageOutcome {
    data object Continue : StageOutcome()
    data class RetryFrom(val stage: Stage, val reason: String) : StageOutcome()
    data class Fatal(val reason: String) : StageOutcome()
}
