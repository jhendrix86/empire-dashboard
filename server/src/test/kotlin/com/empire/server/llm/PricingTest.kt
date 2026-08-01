package com.empire.server.llm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PricingTest {
    @Test
    fun `cost scales up with input and output length`() {
        val small = Pricing.estimateCostUsd("claude-sonnet-5", inputChars = 400, outputChars = 400)
        val large = Pricing.estimateCostUsd("claude-sonnet-5", inputChars = 4000, outputChars = 4000)

        assertTrue(small > 0.0)
        assertTrue(large > small)
    }

    @Test
    fun `unrecognized model falls back to the priciest known rate`() {
        val known = Pricing.estimateCostUsd("claude-opus-5", inputChars = 4000, outputChars = 4000)
        val unknown = Pricing.estimateCostUsd("some-future-model", inputChars = 4000, outputChars = 4000)

        assertEquals(known, unknown)
    }

    @Test
    fun `a cheaper model produces lower cost for identical input`() {
        val mini = Pricing.estimateCostUsd("gpt-4o-mini", inputChars = 4000, outputChars = 4000)
        val sonnet = Pricing.estimateCostUsd("claude-sonnet-5", inputChars = 4000, outputChars = 4000)

        assertTrue(mini < sonnet)
    }

    @Test
    fun `dated model ids match on the bare family token, not just the hyphenated repo default`() {
        val datedHaiku = Pricing.estimateCostUsd("claude-3-5-haiku-20241022", inputChars = 4000, outputChars = 4000)
        val bareHaiku = Pricing.estimateCostUsd("haiku", inputChars = 4000, outputChars = 4000)
        val opusFallback = Pricing.estimateCostUsd("claude-opus-5", inputChars = 4000, outputChars = 4000)

        assertEquals(bareHaiku, datedHaiku)
        assertTrue(datedHaiku < opusFallback) // must not silently fall back to the priciest rate
    }
}
