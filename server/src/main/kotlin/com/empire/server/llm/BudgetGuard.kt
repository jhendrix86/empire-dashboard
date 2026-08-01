package com.empire.server.llm

/** Thrown when a run's estimated LLM spend crosses its configured cap; the orchestrator
 *  surfaces this as a normal stage failure so the run stops instead of continuing to spend. */
class RunBudgetExceededException(message: String) : Exception(message)

/**
 * Tracks estimated spend for the currently active run. Only one run executes at a time
 * (RunOrchestrator.startRun enforces this), so a single mutable accumulator is sufficient
 * -- no per-run keying needed. [onSpendChanged] lets the caller persist the running total
 * (e.g. onto the run manifest) so it survives a server restart mid-run: without that, a
 * restart would hand a resumed run a fresh full budget, and a crash/restart loop could
 * compound spend past the configured cap indefinitely.
 */
class BudgetGuard(
    private val maxCostUsd: Double?,
    private val onSpendChanged: (Double) -> Unit = {}
) {
    @Volatile private var spentUsd: Double = 0.0

    /** Starts a brand-new run's tally at zero. */
    fun reset() {
        spentUsd = 0.0
    }

    /** Rehydrates the tally after a server restart from a previously persisted total. */
    fun seed(persistedSpentUsd: Double) {
        spentUsd = persistedSpentUsd
    }

    fun spent(): Double = spentUsd

    /** Call after every LLM completion; throws once the run's budget is exhausted. */
    @Synchronized
    fun record(costUsd: Double) {
        spentUsd += costUsd
        onSpendChanged(spentUsd)
        val cap = maxCostUsd ?: return
        if (spentUsd > cap) {
            throw RunBudgetExceededException(
                "run exceeded its $%.2f LLM budget (spent ~$%.2f) -- aborting".format(cap, spentUsd)
            )
        }
    }
}
