package com.empire.server.llm

/** Wraps any [LlmClient], estimating the cost of each completion and feeding it to [guard]. */
class CostTrackingLlmClient(
    private val delegate: LlmClient,
    private val model: String,
    private val guard: BudgetGuard
) : LlmClient {
    override suspend fun complete(systemPrompt: String, userPrompt: String): String {
        val result = delegate.complete(systemPrompt, userPrompt)
        val cost = Pricing.estimateCostUsd(
            model = model,
            inputChars = systemPrompt.length + userPrompt.length,
            outputChars = result.length
        )
        guard.record(cost)
        return result
    }
}
