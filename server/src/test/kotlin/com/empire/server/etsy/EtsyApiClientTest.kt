package com.empire.server.etsy

import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertNull

class EtsyApiClientTest {
    private fun newTokenStore(): EtsyTokenStore = EtsyTokenStore(Files.createTempDirectory("empire-test").toFile())

    @Test
    fun `returns null and does not throw when no api key is configured`() {
        val client = EtsyApiClient(apiKey = null, shopId = "123", taxonomyId = 68887994L, tokenStore = newTokenStore())

        val listing = runBlocking { client.createDraftListing("title", "description", 19.0) }

        assertNull(listing)
    }

    @Test
    fun `returns null and does not throw when no shop id is configured`() {
        val client = EtsyApiClient(apiKey = "key", shopId = null, taxonomyId = 68887994L, tokenStore = newTokenStore())

        val listing = runBlocking { client.createDraftListing("title", "description", 19.0) }

        assertNull(listing)
    }

    @Test
    fun `returns null and does not throw when no taxonomy id is configured`() {
        val client = EtsyApiClient(apiKey = "key", shopId = "123", taxonomyId = null, tokenStore = newTokenStore())

        val listing = runBlocking { client.createDraftListing("title", "description", 19.0) }

        assertNull(listing)
    }

    @Test
    fun `returns null and does not throw when no refresh token is available`() {
        val client = EtsyApiClient(apiKey = "key", shopId = "123", taxonomyId = 68887994L, tokenStore = newTokenStore())

        val listing = runBlocking { client.createDraftListing("title", "description", 19.0) }

        assertNull(listing)
    }
}
