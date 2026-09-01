package com.empire.server.testutil

import com.empire.server.shopify.ShopifyClient
import com.empire.server.shopify.ShopifyProduct

class FakeShopifyClient(private val product: ShopifyProduct? = null) : ShopifyClient {
    data class Call(val title: String, val bodyHtml: String, val priceUsd: Double?)

    val calls = mutableListOf<Call>()

    override suspend fun createDraftProduct(title: String, bodyHtml: String, priceUsd: Double?): ShopifyProduct? {
        calls.add(Call(title, bodyHtml, priceUsd))
        return product
    }
}
