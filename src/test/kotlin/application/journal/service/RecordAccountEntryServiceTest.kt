package altak.ledger.application.journal.service

import altak.ledger.CountingTransactionManager
import altak.ledger.NOW
import altak.ledger.TODAY
import altak.ledger.accountFactory
import altak.ledger.application.journal.AmountTooPrecise
import altak.ledger.application.journal.RecordAccountEntryDto
import altak.ledger.domain.Cursor
import altak.ledger.domain.Money
import altak.ledger.domain.account.Account
import altak.ledger.domain.account.AccountReference
import altak.ledger.domain.journal.BalanceQuery
import altak.ledger.domain.journal.BalancesCalculator
import altak.ledger.domain.journal.MovementType
import altak.ledger.domain.journal.PostingFactory
import altak.ledger.fixedClock
import altak.ledger.infrastructure.persistence.InMemoryAccountRepository
import altak.ledger.infrastructure.persistence.InMemoryJournalEntryRepository
import altak.ledger.infrastructure.persistence.RepositoryChartOfAccounts
import altak.ledger.infrastructure.persistence.RepositoryPostingStore
import altak.ledger.journalEntryFactory
import java.math.BigDecimal
import java.util.Currency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RecordAccountEntryServiceTest {

    private val eur = Currency.getInstance("EUR")
    private val jpy = Currency.getInstance("JPY")
    private val clock = fixedClock()
    private val factory = accountFactory(clock)
    private val accounts = InMemoryAccountRepository()
    private val entries = InMemoryJournalEntryRepository()
    private val transactions = CountingTransactionManager()
    private val postings =
        PostingFactory(RepositoryChartOfAccounts(accounts, factory), journalEntryFactory(clock), clock)
    private val service =
        RecordAccountEntryService(accounts, postings, RepositoryPostingStore(accounts, entries), transactions)

    private val alice = factory.forHolder("Alice", eur, AccountReference("ACC-ALICE")).also(accounts::save)

    private fun record(type: MovementType, amount: String, account: Account = alice) =
        service.execute(
            RecordAccountEntry(RecordAccountEntryDto(account.id.toString(), type, BigDecimal(amount))),
        )

    private fun balanceOf(account: Account) =
        BalancesCalculator(accounts, entries).calculate(BalanceQuery(TODAY, account.id), Cursor(50)).items.single().amount

    @Test
    fun `answers with the entry the movement was posted as`() {
        val entry = record(MovementType.DEPOSIT, "10.50")

        assertEquals("Deposit", entry.description)
        assertEquals(NOW, entry.createdAt)
        assertEquals(TODAY, entry.occurredOn)
        assertEquals(eur, entry.currency)
        assertEquals(setOf("ACC-ALICE", "CASH-EUR"), entry.lines.map { it.reference }.toSet())
        assertEquals(listOf("10.50", "10.50"), entry.lines.map { it.amount })
    }

    @Test
    fun `reads the amount in the account's own currency`() {
        val yuki = factory.forHolder("Yuki", jpy, AccountReference("ACC-YUKI")).also(accounts::save)

        record(MovementType.DEPOSIT, "1000", account = yuki)

        assertEquals(Money(1000, jpy), balanceOf(yuki))
        assertFailsWith<AmountTooPrecise> { record(MovementType.DEPOSIT, "1000.50", account = yuki) }
    }

    @Test
    fun `the balance projected onto an account agrees with the journal behind it`() {
        val bob = factory.forHolder("Bob", eur, AccountReference("ACC-BOB")).also(accounts::save)

        record(MovementType.DEPOSIT, "10.00")
        record(MovementType.DEPOSIT, "2.50", account = bob)
        record(MovementType.WITHDRAWAL, "4.00")

        accounts.all(Cursor(50)).items.forEach { account ->
            assertEquals(balanceOf(account), account.balance, "for ${account.reference}")
        }
    }

    @Test
    fun `leaves the ledger untouched when it refuses`() {
        record(MovementType.DEPOSIT, "10.00")

        assertFailsWith<AmountTooPrecise> { record(MovementType.WITHDRAWAL, "0.001") }

        assertEquals(Money(1000, eur), balanceOf(alice))
        assertEquals(1, entries.byAccount(alice.id, Cursor(50)).items.size)
    }

    @Test
    fun `runs in one transaction`() {
        record(MovementType.DEPOSIT, "10.00")

        assertEquals(1, transactions.transactions)
    }
}
