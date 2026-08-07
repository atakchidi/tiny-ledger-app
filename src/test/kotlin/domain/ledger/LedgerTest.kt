package altak.ledger.domain.ledger

import altak.ledger.CountingTransactionManager
import altak.ledger.NOW
import altak.ledger.fixedClock
import altak.ledger.domain.Money
import altak.ledger.domain.account.Account
import altak.ledger.domain.account.AccountType
import java.util.Currency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LedgerTest {

    private val eur = Currency.getInstance("EUR")
    private val usd = Currency.getInstance("USD")
    private val clock = fixedClock()

    private val alice = Account.forHolder("Alice", eur, clock)
    private val cash = Account.forCash(eur, clock)
    private val ledger = Ledger(alice, cash, clock)

    private fun Posting.balanceOf(account: Account) = accounts.single { it.id == account.id }.balance

    @Test
    fun `a deposit raises what the ledger owes and what it holds alike`() {
        val posting = ledger.deposit(Money(1000, eur))

        assertEquals(Money(1000, eur), posting.balanceOf(alice))
        assertEquals(Money(1000, eur), posting.balanceOf(cash))
        assertEquals("Deposit", posting.entry.description)
    }

    @Test
    fun `a withdrawal lowers both alike`() {
        val funded = Ledger(
            alice.copy(balance = Money(1000, eur)),
            cash.copy(balance = Money(1000, eur)),
            clock,
        )

        val posting = funded.withdraw(Money(400, eur))

        assertEquals(Money(600, eur), posting.balanceOf(alice))
        assertEquals(Money(600, eur), posting.balanceOf(cash))
        assertEquals("Withdrawal", posting.entry.description)
    }

    @Test
    fun `a withdrawal may take the holder below zero`() {
        assertEquals(Money(-250, eur), ledger.withdraw(Money(250, eur)).balanceOf(alice))
    }

    @Test
    fun `carries a description of the movement`() {
        assertEquals("Salary", ledger.deposit(Money(1000, eur), "Salary").entry.description)
    }

    @Test
    fun `an account holder's money is a liability, the cash behind it an asset`() {
        assertEquals(AccountType.LIABILITY, alice.type)
        assertEquals(AccountType.ASSET, cash.type)
        assertEquals("Cash EUR", cash.name)
    }

    @Test
    fun `refuses to settle a holder against cash in another currency`() {
        assertFailsWith<Ledger.CurrencyMismatch> { Ledger(alice, Account.forCash(usd, clock), clock) }
    }

    @Test
    fun `refuses an amount in another currency`() {
        assertFailsWith<Account.CurrencyMismatch> { ledger.deposit(Money(100, usd)) }
    }

    @Test
    fun `refuses an amount of nothing`() {
        assertFailsWith<EntryLine.NonPositiveAmount> { ledger.deposit(Money(0, eur)) }
        assertFailsWith<EntryLine.NonPositiveAmount> { ledger.withdraw(Money(-1, eur)) }
    }
}
