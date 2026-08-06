package altak.ledger.domain.ledger

import altak.ledger.Faker
import altak.ledger.domain.LedgerException
import altak.ledger.domain.Money
import altak.ledger.domain.account.AccountId
import altak.ledger.domain.account.AccountRepository
import altak.ledger.domain.account.AccountType
import altak.ledger.domain.currencyOf
import altak.ledger.infrastructure.persistence.InMemoryAccountRepository
import altak.ledger.infrastructure.persistence.InMemoryJournalEntryRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.uuid.Uuid

class LedgerTest {

    private val eur = currencyOf("EUR")
    private val usd = currencyOf("USD")
    private val clock = Faker.clock()

    private val accounts: AccountRepository = InMemoryAccountRepository()
    private val transactions = Faker.CountingTransactionManager()
    private val ledger = Ledger(accounts, InMemoryJournalEntryRepository(), transactions, clock)

    private fun totalFor(type: AccountType) =
        accounts.all()
            .filter { it.type == type && it.currency == eur }
            .fold(Money.zero(eur)) { running, account -> running + account.balance }

    @Test
    fun `an account holder's money is a liability of the ledger`() {
        val alice = ledger.open("Alice", eur)

        assertEquals(AccountType.LIABILITY, alice.type)
        assertEquals(Money.zero(eur), ledger.balanceOf(alice.id))
    }

    @Test
    fun `a deposit raises the balance and the cash the ledger holds`() {
        val alice = ledger.open("Alice", eur)

        ledger.deposit(alice.id, Money(1000, eur))
        val cash = accounts.all().single { it.type == AccountType.ASSET }

        assertEquals(Money(1000, eur), ledger.balanceOf(alice.id))
        assertEquals(Money(1000, eur), ledger.balanceOf(cash.id))
        assertEquals("Cash EUR", cash.name)
    }

    @Test
    fun `a withdrawal lowers the balance`() {
        val alice = ledger.open("Alice", eur)

        ledger.deposit(alice.id, Money(1000, eur))
        ledger.withdraw(alice.id, Money(400, eur))

        assertEquals(Money(600, eur), ledger.balanceOf(alice.id))
    }

    @Test
    fun `a withdrawal may take the balance below zero`() {
        val alice = ledger.open("Alice", eur)

        ledger.withdraw(alice.id, Money(250, eur))

        assertEquals(Money(-250, eur), ledger.balanceOf(alice.id))
    }

    @Test
    fun `what the ledger holds always equals what it owes`() {
        val alice = ledger.open("Alice", eur)
        val bob = ledger.open("Bob", eur)

        ledger.deposit(alice.id, Money(1000, eur))
        ledger.deposit(bob.id, Money(250, eur))
        ledger.withdraw(alice.id, Money(400, eur))

        assertEquals(Money(850, eur), totalFor(AccountType.ASSET))
        assertEquals(totalFor(AccountType.ASSET), totalFor(AccountType.LIABILITY))
    }

    @Test
    fun `each currency gets its own cash account`() {
        val euros = ledger.open("Alice", eur)
        val dollars = ledger.open("Dollars", usd)

        ledger.deposit(euros.id, Money(100, eur))
        ledger.deposit(dollars.id, Money(100, usd))

        assertEquals(2, accounts.all().count { it.type == AccountType.ASSET })
        assertEquals(Money(100, usd), ledger.balanceOf(dollars.id))
    }

    @Test
    fun `history lists the entries touching the account in the order they were recorded`() {
        val alice = ledger.open("Alice", eur)
        val bob = ledger.open("Bob", eur)

        val deposit = ledger.deposit(alice.id, Money(1000, eur))
        ledger.deposit(bob.id, Money(500, eur))
        val withdrawal = ledger.withdraw(alice.id, Money(200, eur))

        assertEquals(listOf(deposit, withdrawal), ledger.historyOf(alice.id))
    }

    @Test
    fun `history is handed back a page at a time`() {
        val alice = ledger.open("Alice", eur)
        val movements = (1..5).map { ledger.deposit(alice.id, Money(it * 100L, eur)) }

        val firstPage = ledger.historyOf(alice.id, Page(limit = 2))
        val secondPage = ledger.historyOf(alice.id, Page(after = firstPage.last().id, limit = 2))
        val lastPage = ledger.historyOf(alice.id, Page(after = secondPage.last().id, limit = 2))

        assertEquals(movements.take(2), firstPage)
        assertEquals(movements.drop(2).take(2), secondPage)
        assertEquals(movements.drop(4), lastPage)
        assertEquals(emptyList(), ledger.historyOf(alice.id, Page(after = lastPage.last().id)))
    }

    @Test
    fun `a page holds a sensible number of entries`() {
        assertFailsWith<LedgerException.InvalidPage> { Page(limit = 0) }
        assertFailsWith<LedgerException.InvalidPage> { Page(limit = Page.MAX_LIMIT + 1) }
    }

    @Test
    fun `every command runs inside a transaction`() {
        val alice = ledger.open("Alice", eur)
        ledger.deposit(alice.id, Money(1000, eur))
        ledger.withdraw(alice.id, Money(400, eur))

        ledger.balanceOf(alice.id)
        ledger.historyOf(alice.id)

        assertEquals(3, transactions.transactions)
    }

    @Test
    fun `an unknown account cannot be used`() {
        val unknown = AccountId(Uuid.generateV7NonMonotonicAt(clock.now()))

        assertFailsWith<LedgerException.AccountNotFound> { ledger.accountOf(unknown) }
        assertFailsWith<LedgerException.AccountNotFound> { ledger.balanceOf(unknown) }
        assertFailsWith<LedgerException.AccountNotFound> { ledger.historyOf(unknown) }
        assertFailsWith<LedgerException.AccountNotFound> { ledger.deposit(unknown, Money(100, eur)) }
    }

    @Test
    fun `an account cannot take a movement in another currency`() {
        val alice = ledger.open("Alice", eur)

        assertFailsWith<LedgerException.CurrencyMismatch> { ledger.deposit(alice.id, Money(100, usd)) }
    }

    @Test
    fun `a rejected movement leaves the balance untouched`() {
        val alice = ledger.open("Alice", eur)
        ledger.deposit(alice.id, Money(1000, eur))

        assertFailsWith<LedgerException.MalformedEntry> { ledger.deposit(alice.id, Money(0, eur)) }

        assertEquals(Money(1000, eur), ledger.balanceOf(alice.id))
        assertEquals(1, ledger.historyOf(alice.id).size)
    }
}
