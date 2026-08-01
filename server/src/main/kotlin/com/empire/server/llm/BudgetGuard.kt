package com.empire.server.llm

/** Thrown when a run's estimated LLM spend crosses its configured cap; the orchestrator
 *  surfaces this as a normal stage failure so the run stops instead of continuing to spend. */
class RunBudgetExceededException(message: String) : Exception(message)

/**
 * Tracks estimated spend for the currently active run. Only one run executes at a time
 * (RunOrchestrator.startRun enforces this), so a single mutable accumulator reset at the
 * start of each run is sufficient -- no per-run keying needed.
 */
class BudgetGuard(private val maxCostUsd: Double?) {
    @Volatile private var spentUsd: Double = 0.0

    fun reset() {
        spentUsd = 0.0
    }

    fun spent(): Double = spentUsd

    /** Call after every LLM completion; throws once the run's budget is exhausted. */
    @Synchronized
    fun record(costUsd: Double) {
        spentUsd += costUsd
        val cap = maxCostUsd ?: return
        if (spentUsd > cap) {
            throw RunBudgetExceededException(
                "run exceeded its $%.2f LLM budget (spent ~$%.2f) -- aborting".format(cap, spentUsd)
            )
        }
    }
}
