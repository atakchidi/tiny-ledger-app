package altak.ledger.domain.account

import altak.ledger.NOW
import altak.ledger.domain.Money
import altak.ledger.domain.entry.Direction
import altak.ledger.domain.entry.EntryLine
import altak.ledger.fixedClock
import java.util.Currency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AccountTest {

    private val eur = Currency.getInstance("EUR")
    private val usd = Currency.getInstance("USD")
    private val clock = fixedClock()

    private val alice = Account.forHolder("Alice", eur, clock)
    private val cash = Account.internal(AccountRole.CASH, eur, clock)

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
    fun `takes its name, type and reference from the role it serves`() {
        assertEquals("CASH-EUR", cash.reference.toString())
        assertEquals("Cash EUR", cash.name)
        assertEquals(AccountType.ASSET, cash.type)
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
    fun `grows on its own normal side and shrinks on the other`() {
        assertEquals(Direction.CREDIT, alice.sideFor(Effect.INCREASE))
        assertEquals(Direction.DEBIT, alice.sideFor(Effect.DECREASE))
        assertEquals(Direction.DEBIT, cash.sideFor(Effect.INCREASE))
        assertEquals(Direction.CREDIT, cash.sideFor(Effect.DECREASE))
    }

    @Test
    fun `draws a line on the side it is asked for`() {
        val line = alice.line(alice.sideFor(Effect.INCREASE), Money(1000, eur))

        assertEquals(alice.id, line.accountId)
        assertEquals(Direction.CREDIT, line.direction)
        assertEquals(Money(1000, eur), line.amount)
    }




    @Test
    fun `refuses an amount in another currency`() {
        assertFailsWith<Account.CurrencyMismatch> { alice.line(Direction.CREDIT, Money(100, usd)) }
    }

    @Test
    fun `refuses a non-positive amount`() {
        assertFailsWith<EntryLine.NonPositiveAmount> { alice.line(Direction.CREDIT, Money(0, eur)) }
        assertFailsWith<EntryLine.NonPositiveAmount> { alice.line(Direction.DEBIT, Money(-1, eur)) }
    }
}
