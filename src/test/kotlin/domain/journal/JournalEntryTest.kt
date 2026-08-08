package altak.ledger.domain.journal

import altak.ledger.CountingTransactionManager
import altak.ledger.NOW
import altak.ledger.TODAY
import altak.ledger.accountFactory
import altak.ledger.fixedClock
import altak.ledger.journalEntryFactory
import altak.ledger.domain.Money
import altak.ledger.domain.account.Account
import altak.ledger.domain.account.AccountReference
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import java.util.Currency
import kotlin.test.Test
import kotlin.test.assertEquals

class JournalEntryTest {

    private val eur = Currency.getInstance("EUR")
    private val usd = Currency.getInstance("USD")
    private val clock = fixedClock()
    private val factory = accountFactory(clock)
    private val journal = journalEntryFactory(clock)

    private val alice = factory.forHolder("Alice", eur, AccountReference("ACC-Alice".uppercase()))
    private val bob = factory.forHolder("Bob", eur, AccountReference("ACC-Bob".uppercase()))
    private val dollars = factory.forHolder("Dollars", usd, AccountReference("ACC-Dollars".uppercase()))

    private fun entryOf(vararg lines: EntryLine) = journal.create("a movement", lines.toList())

    private fun credit(account: Account, minorUnits: Long, currency: Currency = eur) =
        EntryLine(account.id, Direction.CREDIT, Money(minorUnits, currency))

    private fun debit(account: Account, minorUnits: Long, currency: Currency = eur) =
        EntryLine(account.id, Direction.DEBIT, Money(minorUnits, currency))

    @Test
    fun `takes a version 7 id and its time from the clock`() {
        val entry = entryOf(credit(alice, 1000), debit(bob, 1000))

        assertEquals('7', entry.id.toString()[14])
        assertEquals(NOW, entry.createdAt)
        assertEquals(entry.createdAt, entry.updatedAt)
    }

    @Test
    fun `happened today unless it says otherwise`() {
        assertEquals(TODAY, entryOf(credit(alice, 1000), debit(bob, 1000)).occurredOn)
    }

    @Test
    fun `can be dated back to the day it happened`() {
        val lastMonth = TODAY.minus(30, DateTimeUnit.DAY)

        val entry = journal.create("a movement", listOf(credit(alice, 1000), debit(bob, 1000)), lastMonth)

        assertEquals(lastMonth, entry.occurredOn)
        assertEquals(NOW, entry.createdAt)
    }

    @Test
    fun `accepts an entry split across more than two lines`() {
        val entry = entryOf(credit(alice, 600), credit(bob, 400), debit(alice, 1000))

        assertEquals(3, entry.lines.size)
    }

    @Test
    fun `says what it debits and what it credits`() {
        val entry = entryOf(credit(alice, 600), credit(bob, 400), debit(alice, 1000))

        assertEquals(Money(1000, eur), entry.debited)
        assertEquals(Money(1000, eur), entry.credited)
    }

}
