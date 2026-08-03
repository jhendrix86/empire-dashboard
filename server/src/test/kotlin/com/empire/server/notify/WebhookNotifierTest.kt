package com.empire.server.notify

import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class WebhookNotifierTest {
    @Test
    fun `does nothing and does not throw when no webhook url is configured`() = runBlocking {
        WebhookNotifier(webhookUrl = null).notify("test_event", "test message")
    }
}
