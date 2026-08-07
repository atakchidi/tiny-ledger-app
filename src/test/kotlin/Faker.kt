package altak.ledger

import altak.ledger.infrastructure.UuidV7Generator
import kotlin.time.Clock
import kotlin.time.Instant

val NOW: Instant = Instant.parse("2026-08-06T10:00:00Z")

fun fixedClock(instant: Instant = NOW): Clock = object : Clock {
    override fun now(): Instant = instant
}

val ids = UuidV7Generator()
