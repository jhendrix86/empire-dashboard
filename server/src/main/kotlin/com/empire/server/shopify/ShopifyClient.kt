package com.empire.server.shopify

data class ShopifyProduct(
    val id: Long,
    val adminUrl: String
)

interface ShopifyClient {
    /** Creates a DRAFT (never active/live) product listing. Returns null when Shopify is
     *  unconfigured or the request fails -- a listing failure must never break the run. */
    suspend fun createDraftProduct(title: String, bodyHtml: String, priceUsd: Double?): ShopifyProduct?
}
