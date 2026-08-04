package com.empire.server.notify

/** Fire-and-forget outbound alert for events worth knowing about without polling the app. */
interface Notifier {
    suspend fun notify(event: String, message: String)
}
