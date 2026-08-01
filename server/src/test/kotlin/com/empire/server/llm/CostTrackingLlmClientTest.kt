package com.empire.server.llm

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CostTrackingLlmClientTest {
    private class StubClient(private val response: String) : LlmClient {
        override suspend fun complete(systemPrompt: String, userPrompt: String): String = response
    }

    // Block bodies (not `= runBlocking { ... }`) deliberately: an expression body's return
    // type is inferred from the last statement, and assertFailsWith's return value (the
    // caught exception) would otherwise leak out as the test method's return type -- which
    // JUnit5 silently excludes from discovery since @Test methods must return void/Unit.
    @Test
    fun `records estimated cost for each call and returns the delegate's result`() {
        runBlocking {
            val guard = BudgetGuard(maxCostUsd = null)
            val client = CostTrackingLlmClient(StubClient("a".repeat(400)), model = "claude-sonnet-5", guard = guard)

            val result = client.complete("system", "user")

            assertEquals(400, result.length)
            assertTrue(guard.spent() > 0.0)
        }
    }

    @Test
    fun `throws once accumulated calls exceed the run budget`() {
        runBlocking {
            val guard = BudgetGuard(maxCostUsd = 0.00001)
            val client = CostTrackingLlmClient(StubClient("a".repeat(10_000)), model = "claude-opus-5", guard = guard)

            assertFailsWith<RunBudgetExceededException> { client.complete("system", "user") }
        }
    }
}
