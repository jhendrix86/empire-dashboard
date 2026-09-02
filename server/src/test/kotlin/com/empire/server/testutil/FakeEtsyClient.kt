package com.empire.server.testutil

import com.empire.server.etsy.EtsyClient
import com.empire.server.etsy.EtsyListing

class FakeEtsyClient(private val listing: EtsyListing? = null) : EtsyClient {
    data class Call(val title: String, val description: String, val priceUsd: Double?)

    val calls = mutableListOf<Call>()

    override suspend fun createDraftListing(title: String, description: String, priceUsd: Double?): EtsyListing? {
        calls.add(Call(title, description, priceUsd))
        return listing
    }
}
