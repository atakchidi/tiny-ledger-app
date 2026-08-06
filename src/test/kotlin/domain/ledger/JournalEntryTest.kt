package altak.ledger.domain.ledger

import altak.ledger.CountingTransactionManager
import altak.ledger.NOW
import altak.ledger.fixedClock
import altak.ledger.domain.LedgerException
import altak.ledger.domain.Money
import altak.ledger.domain.account.Account
import altak.ledger.domain.account.AccountType
import altak.ledger.domain.currencyOf
import java.util.Currency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class JournalEntryTest {

    private val eur = currencyOf("EUR")
    private val usd = currencyOf("USD")
    private val clock = fixedClock()

    private val alice = Account("Alice", eur, AccountType.LIABILITY, clock)
    private val bob = Account("Bob", eur, AccountType.LIABILITY, clock)
    private val dollars = Account("Dollars", usd, AccountType.LIABILITY, clock)

    private fun entryOf(vararg lines: EntryLine) = JournalEntry("a movement", lines.toList(), clock)

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
    fun `knows which accounts it touches`() {
        val entry = entryOf(credit(alice, 1000), debit(bob, 1000))

        assertTrue(entry.touches(alice.id))
        assertFalse(entry.touches(dollars.id))
    }

    @Test
    fun `accepts an entry split across more than two lines`() {
        val entry = entryOf(credit(alice, 600), credit(bob, 400), debit(alice, 1000))

        assertEquals(3, entry.lines.size)
    }

    @Test
    fun `keeps debits and credits equal`() {
        assertFailsWith<LedgerException.UnbalancedEntry> { entryOf(credit(alice, 1000), debit(bob, 900)) }
    }

    @Test
    fun `refuses lines that all face the same way`() {
        assertFailsWith<LedgerException.UnbalancedEntry> { entryOf(credit(alice, 1000), credit(bob, 1000)) }
    }

    @Test
    fun `needs at least two lines`() {
        assertFailsWith<LedgerException.MalformedEntry> { entryOf(credit(alice, 1000)) }
    }

    @Test
    fun `cannot mix currencies`() {
        assertFailsWith<LedgerException.CurrencyMismatch> {
            entryOf(credit(alice, 1000), debit(dollars, 1000, usd))
        }
    }
}
