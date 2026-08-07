package altak.ledger.application.ledger

import altak.ledger.CountingTransactionManager
import altak.ledger.NOW
import altak.ledger.application.account.AccountNotFound
import altak.ledger.application.ledger.service.Deposit
import altak.ledger.application.ledger.service.DepositService
import altak.ledger.domain.Cursor
import altak.ledger.domain.Money
import altak.ledger.domain.account.Account
import altak.ledger.domain.account.AccountType
import altak.ledger.domain.ledger.EntryLine
import altak.ledger.fixedClock
import altak.ledger.infrastructure.persistence.InMemoryAccountRepository
import altak.ledger.infrastructure.persistence.InMemoryJournalEntryRepository
import java.util.Currency
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DepositServiceTest {

    private val eur = Currency.getInstance("EUR")
    private val jpy = Currency.getInstance("JPY")
    private val clock = fixedClock()
    private val accounts = InMemoryAccountRepository()
    private val entries = InMemoryJournalEntryRepository()
    private val transactions = CountingTransactionManager()
    private val service = DepositService(accounts, entries, transactions, clock)

    private val alice = Account.forHolder("Alice", eur, clock).also(accounts::save)

    private fun deposit(amount: String, description: String? = null) =
        service.execute(Deposit(alice.id.toString(), MovementDto(BigDecimal(amount), description)))

    private fun balanceOf(account: Account) = accounts.byId(account.id)?.balance

    private fun cash() = accounts.cashIn(eur)

    @Test
    fun `records a balanced entry against the cash the ledger holds`() {
        val entry = deposit("10.50")

        assertEquals("Deposit", entry.description)
        assertEquals(NOW.toString(), entry.createdAt)
        assertEquals(listOf(BigDecimal("10.50"), BigDecimal("10.50")), entry.lines.map { it.amount })
        assertEquals("CREDIT", entry.lines.single { it.accountId == alice.id.toString() }.direction)
        assertEquals("DEBIT", entry.lines.single { it.accountId != alice.id.toString() }.direction)
    }

    @Test
    fun `raises what the ledger owes and what it holds alike`() {
        deposit("10.50")

        assertEquals(Money(1050, eur), balanceOf(alice))
        assertEquals(Money(1050, eur), cash()?.balance)
        assertEquals("Cash EUR", cash()?.name)
        assertEquals(AccountType.ASSET, cash()?.type)
    }

    @Test
    fun `opens the cash account once and settles later deposits against it`() {
        deposit("10.00")
        deposit("5.00")

        assertEquals(Money(1500, eur), cash()?.balance)
        assertEquals(2, accounts.all(Cursor()).size)
    }

    @Test
    fun `keeps the entry where both accounts can find it`() {
        val entry = deposit("10.00")

        assertEquals(listOf(entry.id), entries.byAccount(alice.id, Cursor()).map { it.id.toString() })
        assertEquals(listOf(entry.id), entries.byAccount(cash()!!.id, Cursor()).map { it.id.toString() })
    }

    @Test
    fun `carries the caller's own description`() {
        assertEquals("Salary", deposit("10.00", "Salary").description)
    }

    @Test
    fun `counts amounts in the currency's own precision`() {
        val yuki = Account.forHolder("Yuki", jpy, clock).also(accounts::save)

        service.execute(Deposit(yuki.id.toString(), MovementDto(BigDecimal("1000"))))

        assertEquals(Money(1000, jpy), balanceOf(yuki))
    }

    @Test
    fun `refuses an amount finer than the currency allows`() {
        assertFailsWith<Money.MalformedAmount> { deposit("10.505") }
    }

    @Test
    fun `refuses an amount of nothing`() {
        assertFailsWith<EntryLine.NonPositiveAmount> { deposit("0.00") }
    }

    @Test
    fun `leaves the ledger untouched when it refuses`() {
        deposit("10.00")

        assertFailsWith<EntryLine.NonPositiveAmount> { deposit("0.00") }

        assertEquals(Money(1000, eur), balanceOf(alice))
        assertEquals(1, entries.byAccount(alice.id, Cursor()).size)
    }

    @Test
    fun `refuses an id that names no account`() {
        assertFailsWith<AccountNotFound> {
            service.execute(Deposit("not-an-account", MovementDto(BigDecimal("10.00"))))
        }
    }

    @Test
    fun `runs in one transaction`() {
        deposit("10.00")

        assertEquals(1, transactions.transactions)
    }
}
