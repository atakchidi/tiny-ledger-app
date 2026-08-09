package altak.ledger.domain.journal

import altak.ledger.NOW
import altak.ledger.TODAY
import altak.ledger.accountFactory
import altak.ledger.domain.Money
import altak.ledger.domain.account.Account
import altak.ledger.domain.account.AccountReference
import altak.ledger.fixedClock
import java.util.Currency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

class JournalEntryTest {

    private val eur = Currency.getInstance("EUR")
    private val factory = accountFactory(fixedClock())

    private val alice = factory.forHolder("Alice", eur, AccountReference("ACC-ALICE"))
    private val bob = factory.forHolder("Bob", eur, AccountReference("ACC-BOB"))

    private fun entryOf(vararg lines: EntryLine) =
        JournalEntry(EntryId(Uuid.random()), "a movement", TODAY, NOW, lines.toList())

    private fun credit(account: Account, minorUnits: Long, currency: Currency = eur) =
        EntryLine(account.id, Direction.CREDIT, Money(minorUnits, currency))

    private fun debit(account: Account, minorUnits: Long, currency: Currency = eur) =
        EntryLine(account.id, Direction.DEBIT, Money(minorUnits, currency))

    @Test
    fun `says what it debits and what it credits, and the currency they are in`() {
        val entry = entryOf(credit(alice, 600), credit(bob, 400), debit(alice, 1000))

        assertEquals(Money(1000, eur), entry.debited)
        assertEquals(Money(1000, eur), entry.credited)
        assertEquals(eur, entry.currency)
    }

    @Test
    fun `accepts an entry split across more than two lines`() {
        assertEquals(3, entryOf(credit(alice, 600), credit(bob, 400), debit(alice, 1000)).lines.size)
    }

    @Test
    fun `reads a line against the side the account is normally kept on`() {
        assertEquals(Money(1000, eur), credit(alice, 1000).signedAgainst(Direction.CREDIT))
        assertEquals(Money(-1000, eur), credit(alice, 1000).signedAgainst(Direction.DEBIT))
    }

    @Test
    fun `every direction has an opposite`() {
        assertEquals(Direction.CREDIT, Direction.DEBIT.opposite)
        assertEquals(Direction.DEBIT, Direction.CREDIT.opposite)
    }
}
