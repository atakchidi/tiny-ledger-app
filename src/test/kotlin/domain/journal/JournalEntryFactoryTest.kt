package altak.ledger.domain.journal

import altak.ledger.NOW
import altak.ledger.TODAY
import altak.ledger.accountFactory
import altak.ledger.domain.Money
import altak.ledger.domain.account.AccountReference
import altak.ledger.fixedClock
import altak.ledger.journalEntryFactory
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import java.util.Currency
import kotlin.test.Test
import kotlin.test.assertEquals

class JournalEntryFactoryTest {

    private val eur = Currency.getInstance("EUR")
    private val clock = fixedClock()
    private val factory = accountFactory(clock)
    private val journal = journalEntryFactory(clock)

    private val alice = factory.forHolder("Alice", eur, AccountReference("ACC-ALICE"))
    private val bob = factory.forHolder("Bob", eur, AccountReference("ACC-BOB"))

    private val balanced = listOf(
        EntryLine(alice.id, Direction.CREDIT, Money(1000, eur)),
        EntryLine(bob.id, Direction.DEBIT, Money(1000, eur)),
    )

    @Test
    fun `takes a version 7 id and the moment it was recorded from the clock`() {
        val entry = journal.create("a movement", balanced)

        assertEquals('7', entry.id.toString()[14])
        assertEquals(NOW, entry.createdAt)
    }

    @Test
    fun `happened today unless it says otherwise`() {
        assertEquals(TODAY, journal.create("a movement", balanced).occurredOn)
    }

    @Test
    fun `can be dated back to the day it happened, without moving when it was recorded`() {
        val lastMonth = TODAY.minus(30, DateTimeUnit.DAY)

        val entry = journal.create("a movement", balanced, lastMonth)

        assertEquals(lastMonth, entry.occurredOn)
        assertEquals(NOW, entry.createdAt)
    }
}
