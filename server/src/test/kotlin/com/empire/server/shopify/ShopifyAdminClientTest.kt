package com.empire.server.shopify

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertNull

class ShopifyAdminClientTest {
    @Test
    fun `returns null and does not throw when no store credentials are configured`() {
        val product = runBlocking {
            ShopifyAdminClient(storeDomain = null, accessToken = "token").createDraftProduct("title", "body", 19.0)
        }

        assertNull(product)
    }

    @Test
    fun `returns null and does not throw when no access token is configured`() {
        val product = runBlocking {
            ShopifyAdminClient(storeDomain = "store.myshopify.com", accessToken = null).createDraftProduct("title", "body", 19.0)
        }

        assertNull(product)
    }
}
