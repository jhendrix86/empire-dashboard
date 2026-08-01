package com.empire.server.llm

/**
 * Rough public USD-per-1M-token rates, used only to estimate spend for the run budget
 * guardrail -- not for billing. Falls back to the priciest known rate for an unrecognized
 * model so an unknown/future model errs toward tripping the cap sooner rather than later.
 */
object Pricing {
    private val ratesPerMillionTokens = listOf(
        "sonnet" to (3.00 to 15.00),
        "opus" to (15.00 to 75.00),
        "haiku" to (0.80 to 4.00),
        "gpt-4o-mini" to (0.15 to 0.60),
        "gpt-4o" to (2.50 to 10.00)
    )
    private val fallbackRatePerMillionTokens = 15.00 to 75.00
    private const val CHARS_PER_TOKEN = 4.0

    fun estimateCostUsd(model: String, inputChars: Int, outputChars: Int): Double {
        val (inputRatePerMillion, outputRatePerMillion) = ratesPerMillionTokens
            .firstOrNull { (key, _) -> model.contains(key, ignoreCase = true) }
            ?.second
            ?: fallbackRatePerMillionTokens
        val inputTokens = inputChars / CHARS_PER_TOKEN
        val outputTokens = outputChars / CHARS_PER_TOKEN
        return (inputTokens / 1_000_000.0) * inputRatePerMillion +
            (outputTokens / 1_000_000.0) * outputRatePerMillion
    }
}
