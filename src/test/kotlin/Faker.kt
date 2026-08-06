package altak.ledger

import kotlin.time.Clock
import kotlin.time.Instant

object Faker {

    val NOW: Instant = Instant.parse("2026-08-06T10:00:00Z")

    fun clock(now: Instant = NOW): Clock = FakeClock(now)

    private class FakeClock(private val instant: Instant) : Clock {
        override fun now(): Instant = instant
    }
}
