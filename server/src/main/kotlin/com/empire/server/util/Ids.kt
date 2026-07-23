package com.empire.server.util

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.random.Random

private val runIdFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC)

fun newRunId(): String {
    val timestamp = runIdFormatter.format(Instant.now())
    val suffix = Random.nextInt(0, 0xFFFF).toString(16).padStart(4, '0')
    return "run-$timestamp-$suffix"
}
