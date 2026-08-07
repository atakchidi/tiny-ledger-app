package altak.ledger.domain.account

import altak.ledger.CountingTransactionManager
import altak.ledger.NOW
import altak.ledger.fixedClock
import altak.ledger.domain.Money
import altak.ledger.domain.ledger.Direction
import altak.ledger.domain.ledger.EntryLine
import altak.ledger.domain.ledger.JournalEntry
import java.util.Currency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.hours

class AccountTest {

    private val eur = Currency.getInstance("EUR")
    private val usd = Currency.getInstance("USD")
    private val clock = fixedClock()

    private val alice = Account.forHolder("Alice", eur, clock)
    private val cash = Account.forCash(eur, clock)

    private fun JournalEntry.lineFor(account: Account) = lines.single { it.accountId == account.id }

    @Test
    fun `takes its creation time and a version 7 id from the clock`() {
        assertEquals(NOW, alice.createdAt)
        assertEquals('7', alice.id.toString()[14])
    }

    @Test
    fun `is given a reference when the holder brings none`() {
        assertEquals("ACC-${alice.id.value.toString().takeLast(12).uppercase()}", alice.reference.toString())
    }

    @Test
    fun `keeps the reference the holder brought, in canonical form`() {
        val named = Account.forHolder("Alice", eur, clock, AccountReference.normalized("  acc-000123 "))

        assertEquals("ACC-000123", named.reference.toString())
    }

    @Test
    fun `refers to cash by the currency it holds`() {
        assertEquals("CASH-EUR", cash.reference.toString())
    }

    @Test
    fun `refuses a reference nothing could quote back`() {
        assertFailsWith<AccountReference.Malformed> { AccountReference.normalized("no") }
        assertFailsWith<AccountReference.Malformed> { AccountReference.normalized("-leading-dash") }
        assertFailsWith<AccountReference.Malformed> { AccountReference.normalized("with spaces") }
        assertFailsWith<AccountReference.Malformed> { AccountReference.normalized("a".repeat(33)) }
    }

    @Test
    fun `has not been touched since it was opened`() {
        assertEquals(alice.createdAt, alice.updatedAt)
    }

    @Test
    fun `is stamped as updated when it records a line`() {
        val later = fixedClock(NOW.plus(1.hours))

        val recorded = alice.record(alice.deposit(Money(1000, eur), cash, clock, "Deposit").lineFor(alice), later)

        assertEquals(NOW, recorded.createdAt)
        assertEquals(NOW.plus(1.hours), recorded.updatedAt)
    }

    @Test
    fun `starts with a zero balance in its own currency`() {
        assertEquals(Money.zero(eur), alice.balance)
    }

    @Test
    fun `a deposit increases both sides of the books`() {
        val entry = alice.deposit(Money(1000, eur), cash, clock, "Deposit")

        assertEquals(Direction.CREDIT, entry.lineFor(alice).direction)
        assertEquals(Direction.DEBIT, entry.lineFor(cash).direction)
        assertEquals("Deposit", entry.description)
        assertEquals(NOW, entry.createdAt)
    }

    @Test
    fun `a withdrawal decreases both sides of the books`() {
        val entry = alice.withdraw(Money(400, eur), cash, clock, "Withdrawal")

        assertEquals(Direction.DEBIT, entry.lineFor(alice).direction)
        assertEquals(Direction.CREDIT, entry.lineFor(cash).direction)
        assertEquals("Withdrawal", entry.description)
    }

    @Test
    fun `carries a description of the movement`() {
        assertEquals("Salary", alice.deposit(Money(1000, eur), cash, clock, "Salary").description)
    }

    @Test
    fun `recording a line moves the balance in the direction the line faces`() {
        val deposited = alice.record(alice.deposit(Money(1000, eur), cash, clock, "Deposit").lineFor(alice), clock)
        val withdrawn = deposited.record(deposited.withdraw(Money(400, eur), cash, clock, "Withdrawal").lineFor(alice), clock)

        assertEquals(Money(1000, eur), deposited.balance)
        assertEquals(Money(600, eur), withdrawn.balance)
    }

    @Test
    fun `the cash side of a deposit rises just as the holder's does`() {
        val entry = alice.deposit(Money(1000, eur), cash, clock, "Deposit")

        assertEquals(Money(1000, eur), cash.record(entry.lineFor(cash), clock).balance)
    }

    @Test
    fun `refuses to record a line belonging to another account`() {
        val entry = alice.deposit(Money(1000, eur), cash, clock, "Deposit")

        assertFailsWith<Account.ForeignLine> { alice.record(entry.lineFor(cash), clock) }
    }

    @Test
    fun `a line counts towards the balance in the direction it faces`() {
        val credited = EntryLine(alice.id, Direction.CREDIT, Money(1000, eur))
        val debited = EntryLine(alice.id, Direction.DEBIT, Money(1000, eur))

        assertEquals(Money(1000, eur), credited.signedAgainst(alice.type.normalSide))
        assertEquals(Money(-1000, eur), debited.signedAgainst(alice.type.normalSide))
    }

    @Test
    fun `refuses an amount in another currency`() {
        assertFailsWith<Account.CurrencyMismatch> { alice.deposit(Money(100, usd), cash, clock, "Deposit") }
        assertFailsWith<Account.CurrencyMismatch> { alice.withdraw(Money(100, usd), cash, clock, "Withdrawal") }
    }

    @Test
    fun `refuses a non-positive amount`() {
        assertFailsWith<EntryLine.NonPositiveAmount> { alice.deposit(Money(0, eur), cash, clock, "Deposit") }
        assertFailsWith<EntryLine.NonPositiveAmount> { alice.withdraw(Money(-1, eur), cash, clock, "Withdrawal") }
    }
}
