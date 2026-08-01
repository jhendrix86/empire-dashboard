package com.empire.server.llm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
}
