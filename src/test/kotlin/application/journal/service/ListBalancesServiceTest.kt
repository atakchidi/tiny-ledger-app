package altak.ledger.application.journal.service

import altak.ledger.CountingTransactionManager
import altak.ledger.TODAY
import altak.ledger.accountFactory
import altak.ledger.advancingClock
import altak.ledger.application.journal.SearchBalancesDto
import altak.ledger.application.shared.CursorDto
import altak.ledger.calendar
import altak.ledger.domain.Money
import altak.ledger.domain.account.AccountReference
import altak.ledger.domain.account.AccountRole
import altak.ledger.domain.journal.BalancesCalculator
import altak.ledger.domain.journal.Direction
import altak.ledger.domain.journal.EntryLine
import altak.ledger.fixedClock
import altak.ledger.infrastructure.persistence.InMemoryAccountRepository
import altak.ledger.infrastructure.persistence.InMemoryJournalEntryRepository
import altak.ledger.journalEntryFactory
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import java.util.Currency
import kotlin.test.Test
import kotlin.test.assertEquals

class ListBalancesServiceTest {

    private val eur = Currency.getInstance("EUR")
    private val clock = fixedClock()
    private val factory = accountFactory(clock)
    private val journal = journalEntryFactory(advancingClock())
    private val accounts = InMemoryAccountRepository()
    private val entries = InMemoryJournalEntryRepository()
    private val transactions = CountingTransactionManager()
    private val service =
        ListBalancesService(accounts, BalancesCalculator(accounts, entries), transactions, calendar(clock))

    private val alice = factory.forHolder("Alice", eur, AccountReference("ACC-ALICE")).also(accounts::save)
    private val cash = factory.internal(AccountRole.CASH, eur).also(accounts::save)

    private fun deposit(minorUnits: Long, occurredOn: LocalDate? = null) =
        journal.create(
            "Deposit",
            listOf(
                EntryLine(alice.id, Direction.CREDIT, Money(minorUnits, eur)),
                EntryLine(cash.id, Direction.DEBIT, Money(minorUnits, eur)),
            ),
            occurredOn,
        ).also(entries::save)

    private fun balances(account: String? = null, onDate: LocalDate? = null) =
        service.execute(ListBalances(SearchBalancesDto(account, onDate), CursorDto()))

    @Test
    fun `shows what an account stands at, in the currency it is held in`() {
        deposit(1050)

        val balance = balances("ACC-ALICE").items.single()

        assertEquals(alice.id.toString(), balance.accountId)
        assertEquals("ACC-ALICE", balance.reference)
        assertEquals(eur, balance.currency)
        assertEquals("10.50", balance.amount)
    }

    @Test
    fun `answers for every account when none is named`() {
        deposit(1050)

        assertEquals(setOf("ACC-ALICE", "CASH-EUR"), balances().items.map { it.reference }.toSet())
    }

    @Test
    fun `reads the journal as of today unless the caller names a date`() {
        val lastWeek = TODAY.minus(7, DateTimeUnit.DAY)
        deposit(1000, lastWeek)
        deposit(400)

        assertEquals(TODAY, balances("ACC-ALICE").items.single().onDate)
        assertEquals("14.00", balances("ACC-ALICE").items.single().amount)
        assertEquals(lastWeek, balances("ACC-ALICE", lastWeek).items.single().onDate)
        assertEquals("10.00", balances("ACC-ALICE", lastWeek).items.single().amount)
    }

    @Test
    fun `runs in one transaction`() {
        balances()

        assertEquals(1, transactions.transactions)
    }
}
