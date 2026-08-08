package altak.ledger

import altak.ledger.domain.LedgerCalendar
import altak.ledger.domain.account.AccountFactory
import altak.ledger.domain.journal.JournalEntryFactory
import altak.ledger.infrastructure.UuidV7Generator
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

val NOW: Instant = Instant.parse("2026-08-06T10:00:00Z")

val ZONE: TimeZone = TimeZone.of("Europe/Riga")

val TODAY: LocalDate = NOW.toLocalDateTime(ZONE).date

fun fixedClock(instant: Instant = NOW): Clock = object : Clock {
    override fun now(): Instant = instant
}

// Version 7 ids only sort by creation while the clock moves, which a fixed one does not.
fun advancingClock(start: Instant = NOW, step: Duration = 1.seconds): Clock = object : Clock {
    private var next = start

    override fun now(): Instant = next.also { next += step }
}

val ids = UuidV7Generator()

fun accountFactory(clock: Clock = fixedClock()) = AccountFactory(ids, clock)

fun calendar(clock: Clock = fixedClock()) = LedgerCalendar(clock, ZONE)

fun journalEntryFactory(clock: Clock = fixedClock()) = JournalEntryFactory(ids, calendar(clock), clock)
