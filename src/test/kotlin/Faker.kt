package altak.ledger

import altak.ledger.domain.TransactionManager
import kotlin.time.Clock
import kotlin.time.Instant

object Faker {

    val NOW: Instant = Instant.parse("2026-08-06T10:00:00Z")

    fun clock(now: Instant = NOW): Clock = FakeClock(now)

    class CountingTransactionManager : TransactionManager {
        var transactions = 0
            private set

        override fun <T> invoke(work: () -> T): T {
            transactions++
            return work()
        }
    }

    private class FakeClock(private val instant: Instant) : Clock {
        override fun now(): Instant = instant
    }
}
