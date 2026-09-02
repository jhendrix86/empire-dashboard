package com.empire.server.etsy

data class EtsyListing(
    val listingId: Long,
    val url: String
)

interface EtsyClient {
    /** Creates a DRAFT listing (Etsy's own createDraftListing endpoint -- listings it
     *  produces are never active/live until the operator publishes them). Returns null
     *  when Etsy is unconfigured or the request fails -- a listing failure must never
     *  break the run. */
    suspend fun createDraftListing(title: String, description: String, priceUsd: Double?): EtsyListing?
}
