package com.empire.server.testutil

import com.empire.server.notify.Notifier

/** Records every [notify] call instead of sending anything, so tests can assert on them. */
class FakeNotifier : Notifier {
    data class Sent(val event: String, val message: String)

    private val _sent = mutableListOf<Sent>()
    val sent: List<Sent> get() = _sent

    override suspend fun notify(event: String, message: String) {
        _sent.add(Sent(event, message))
    }
}
