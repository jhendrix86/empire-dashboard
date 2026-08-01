package com.empire.server.llm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class BudgetGuardTest {
    @Test
    fun `no cap never throws regardless of spend`() {
        val guard = BudgetGuard(maxCostUsd = null)

        guard.record(1_000_000.0)

        assertEquals(1_000_000.0, guard.spent())
    }

    @Test
    fun `throws once cumulative spend exceeds the cap`() {
        val guard = BudgetGuard(maxCostUsd = 1.0)
        guard.record(0.6)

        assertFailsWith<RunBudgetExceededException> { guard.record(0.6) }
    }

    @Test
    fun `stays under the cap does not throw`() {
        val guard = BudgetGuard(maxCostUsd = 1.0)

        guard.record(0.4)
        guard.record(0.4)

        assertEquals(0.8, guard.spent())
    }

    @Test
    fun `reset clears accumulated spend so a new run starts fresh`() {
        val guard = BudgetGuard(maxCostUsd = 1.0)
        guard.record(0.9)

        guard.reset()

        assertEquals(0.0, guard.spent())
        guard.record(0.9) // would have thrown before reset
    }

    @Test
    fun `seed sets the tally directly without notifying the change callback`() {
        var notified = false
        val guard = BudgetGuard(maxCostUsd = null, onSpendChanged = { notified = true })

        guard.seed(0.42)

        assertEquals(0.42, guard.spent())
        assertFalse(notified)
    }

    @Test
    fun `seeding near the cap makes the very next call throw, closing the restart-loop gap`() {
        val guard = BudgetGuard(maxCostUsd = 1.0)
        guard.seed(0.95) // e.g. rehydrated from a manifest after a server restart mid-run

        assertFailsWith<RunBudgetExceededException> { guard.record(0.2) }
    }

    @Test
    fun `record notifies the change callback with the running total after each call`() {
        val seen = mutableListOf<Double>()
        val guard = BudgetGuard(maxCostUsd = null, onSpendChanged = seen::add)

        guard.record(0.1)
        guard.record(0.2)

        assertEquals(2, seen.size)
        assertEquals(guard.spent(), seen.last())
    }
}
