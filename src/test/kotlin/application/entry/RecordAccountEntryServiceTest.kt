package altak.ledger.application.entry

import altak.ledger.CountingTransactionManager
import altak.ledger.NOW
import altak.ledger.application.account.AccountNotFound
import altak.ledger.application.entry.service.RecordAccountEntry
import altak.ledger.application.entry.service.RecordAccountEntryService
import altak.ledger.domain.Cursor
import altak.ledger.domain.Money
import altak.ledger.domain.account.Account
import altak.ledger.domain.account.AccountRole
import altak.ledger.domain.account.AccountType
import altak.ledger.domain.entry.Direction
import altak.ledger.domain.entry.EntryLine
import altak.ledger.domain.entry.BalanceQuery
import altak.ledger.domain.entry.BalancesCalculator
import altak.ledger.domain.entry.MovementType
import altak.ledger.domain.entry.PostingFactory
import altak.ledger.fixedClock
import altak.ledger.infrastructure.persistence.InMemoryAccountRepository
import altak.ledger.infrastructure.persistence.InMemoryJournalEntryRepository
import altak.ledger.infrastructure.persistence.RepositoryChartOfAccounts
import altak.ledger.infrastructure.persistence.RepositoryPostingStore
import java.math.BigDecimal
import java.util.Currency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RecordAccountEntryServiceTest {

    private val eur = Currency.getInstance("EUR")
    private val jpy = Currency.getInstance("JPY")
    private val clock = fixedClock()
    private val accounts = InMemoryAccountRepository()
    private val entries = InMemoryJournalEntryRepository()
    private val transactions = CountingTransactionManager()
    private val postings = PostingFactory(RepositoryChartOfAccounts(accounts, clock), clock)
    private val service = RecordAccountEntryService(accounts, postings, RepositoryPostingStore(accounts, entries), transactions)

    private val alice = Account.forHolder("Alice", eur, clock).also(accounts::save)

    private fun record(
        type: MovementType,
        amount: String,
        description: String? = null,
        account: Account = alice,
    ) = service.execute(RecordAccountEntry(account.id.toString(), MovementDto(type, BigDecimal(amount), description)))

    private fun balanceOf(account: Account) =
        BalancesCalculator(accounts, entries)
            .calculate(BalanceQuery(NOW, account.id), Cursor())
            .items
            .single()
            .amount

    private fun cash() = accounts.byReference(AccountRole.CASH.referenceFor(eur))

    @Test
    fun `a deposit is recorded as a balanced entry against the cash the ledger holds`() {
        val entry = record(MovementType.DEPOSIT, "10.50")

        assertEquals("Deposit", entry.description)
        assertEquals(NOW.toString(), entry.createdAt)
        assertEquals(listOf(BigDecimal("10.50"), BigDecimal("10.50")), entry.lines.map { it.amount })
        assertEquals("CREDIT", entry.lines.single { it.accountId == alice.id.toString() }.direction)
        assertEquals("DEBIT", entry.lines.single { it.accountId != alice.id.toString() }.direction)
    }

    @Test
    fun `a deposit raises what the ledger owes and what it holds alike`() {
        record(MovementType.DEPOSIT, "10.50")

        assertEquals(Money(1050, eur), balanceOf(alice))
        assertEquals(Money(1050, eur), balanceOf(cash()!!))
        assertEquals("Cash EUR", cash()?.name)
        assertEquals(AccountType.ASSET, cash()?.type)
    }

    @Test
    fun `a withdrawal turns the entry around and lowers both`() {
        record(MovementType.DEPOSIT, "10.50")
        val entry = record(MovementType.WITHDRAWAL, "4.00")

        assertEquals("Withdrawal", entry.description)
        assertEquals("DEBIT", entry.lines.single { it.accountId == alice.id.toString() }.direction)
        assertEquals(Money(650, eur), balanceOf(alice))
        assertEquals(Money(650, eur), balanceOf(cash()!!))
    }

    @Test
    fun `a withdrawal may take the holder below zero`() {
        record(MovementType.WITHDRAWAL, "2.50")

        assertEquals(Money(-250, eur), balanceOf(alice))
    }

    @Test
    fun `opens the cash account once and settles later movements against it`() {
        record(MovementType.DEPOSIT, "10.00")
        record(MovementType.DEPOSIT, "5.00")

        assertEquals(Money(1500, eur), balanceOf(cash()!!))
        assertEquals(2, accounts.all(Cursor()).items.size)
    }

    @Test
    fun `keeps the entry where both accounts can find it`() {
        val entry = record(MovementType.DEPOSIT, "10.00")

        assertEquals(listOf(entry.id), entries.byAccount(alice.id, Cursor()).items.map { it.id.toString() })
        assertEquals(listOf(entry.id), entries.byAccount(cash()!!.id, Cursor()).items.map { it.id.toString() })
    }

    @Test
    fun `keeps total debits equal to total credits across a run of movements`() {
        val bob = Account.forHolder("Bob", eur, clock).also(accounts::save)

        record(MovementType.DEPOSIT, "10.00")
        record(MovementType.DEPOSIT, "2.50", account = bob)
        record(MovementType.WITHDRAWAL, "4.00")

        val lines = accounts.all(Cursor()).items
            .flatMap { entries.byAccount(it.id, Cursor()).items }
            .distinct()
            .flatMap { it.lines }
        val debited = lines.filter { it.direction == Direction.DEBIT }.sumOf { it.amount.minorUnits }
        val credited = lines.filter { it.direction == Direction.CREDIT }.sumOf { it.amount.minorUnits }

        assertEquals(debited, credited)
        assertEquals(850, balanceOf(alice).minorUnits + balanceOf(bob).minorUnits)
        assertEquals(850, balanceOf(cash()!!).minorUnits)
    }

    @Test
    fun `the balance projected onto an account agrees with the journal`() {
        val bob = Account.forHolder("Bob", eur, clock).also(accounts::save)

        record(MovementType.DEPOSIT, "10.00")
        record(MovementType.DEPOSIT, "2.50", account = bob)
        record(MovementType.WITHDRAWAL, "4.00")

        accounts.all(Cursor()).items.forEach { account ->
            assertEquals(balanceOf(account), account.balance, "for ${account.reference}")
        }
    }

    @Test
    fun `carries the caller's own description`() {
        assertEquals("Salary", record(MovementType.DEPOSIT, "10.00", "Salary").description)
        assertEquals("Rent", record(MovementType.WITHDRAWAL, "4.00", "Rent").description)
    }

    @Test
    fun `counts amounts in the currency's own precision`() {
        val yuki = Account.forHolder("Yuki", jpy, clock).also(accounts::save)

        record(MovementType.DEPOSIT, "1000", account = yuki)

        assertEquals(Money(1000, jpy), balanceOf(yuki))
    }

    @Test
    fun `refuses an amount finer than the currency allows`() {
        assertFailsWith<Money.MalformedAmount> { record(MovementType.DEPOSIT, "10.505") }
    }

    @Test
    fun `refuses an amount of nothing`() {
        assertFailsWith<EntryLine.NonPositiveAmount> { record(MovementType.DEPOSIT, "0.00") }
    }

    @Test
    fun `leaves the ledger untouched when it refuses`() {
        record(MovementType.DEPOSIT, "10.00")

        assertFailsWith<EntryLine.NonPositiveAmount> { record(MovementType.WITHDRAWAL, "0.00") }

        assertEquals(Money(1000, eur), balanceOf(alice))
        assertEquals(1, entries.byAccount(alice.id, Cursor()).items.size)
    }

    @Test
    fun `refuses an id that names no account`() {
        assertFailsWith<AccountNotFound> {
            service.execute(RecordAccountEntry("not-an-account", MovementDto(MovementType.DEPOSIT, BigDecimal("10.00"))))
        }
    }

    @Test
    fun `runs in one transaction`() {
        record(MovementType.DEPOSIT, "10.00")

        assertEquals(1, transactions.transactions)
    }
}
