package altak.ledger.domain.journal

import altak.ledger.TODAY
import altak.ledger.domain.Cursor
import altak.ledger.domain.Money
import altak.ledger.domain.account.Account
import altak.ledger.domain.account.AccountReference
import altak.ledger.domain.account.AccountRole
import altak.ledger.accountFactory
import altak.ledger.fixedClock
import altak.ledger.journalEntryFactory
import altak.ledger.infrastructure.persistence.InMemoryAccountRepository
import altak.ledger.infrastructure.persistence.InMemoryJournalEntryRepository
import altak.ledger.infrastructure.persistence.RepositoryChartOfAccounts
import altak.ledger.infrastructure.persistence.RepositoryPostingStore
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.DateTimeUnit
import java.util.Currency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BalancesCalculatorTest {

    private val eur = Currency.getInstance("EUR")
    private val factory = accountFactory()
    private val accounts = InMemoryAccountRepository()
    private val entries = InMemoryJournalEntryRepository()
    private val store = RepositoryPostingStore(accounts, entries)
    private val calculator = BalancesCalculator(accounts, entries)

    private val alice = factory.forHolder("Alice", eur, AccountReference("ACC-Alice".uppercase())).also(accounts::save)
    private val bob = factory.forHolder("Bob", eur, AccountReference("ACC-Bob".uppercase())).also(accounts::save)

    private fun record(account: Account, movement: MovementType, minorUnits: Long, on: LocalDate = TODAY) {
        val chart = RepositoryChartOfAccounts(accounts, factory)

        store.store(
            PostingFactory(chart, journalEntryFactory(), fixedClock())
                .create(account, movement, Money(minorUnits, eur), occurredOn = on),
        )
    }

    private fun daysBeforeToday(days: Int) = TODAY.minus(days, DateTimeUnit.DAY)

    private fun balanceOf(account: Account, onDate: LocalDate = TODAY) =
        calculator.calculate(BalanceQuery(onDate, account.id), Cursor(50)).items.single().amount

    @Test
    fun `an account with nothing in the journal owes nothing`() {
        assertEquals(Money.zero(eur), balanceOf(alice))
    }

    @Test
    fun `folds every line the journal holds for the account`() {
        record(alice, MovementType.DEPOSIT, 1000)
        record(alice, MovementType.WITHDRAWAL, 400)

        assertEquals(Money(600, eur), balanceOf(alice))
    }

    @Test
    fun `reads the journal as it stood on the date it is asked for`() {
        val lastMonth = daysBeforeToday(30)
        val yesterday = daysBeforeToday(1)

        record(alice, MovementType.DEPOSIT, 1000, lastMonth)
        record(alice, MovementType.DEPOSIT, 250, yesterday)

        assertEquals(Money.zero(eur), balanceOf(alice, daysBeforeToday(31)))
        assertEquals(Money(1000, eur), balanceOf(alice, lastMonth))
        assertEquals(Money(1000, eur), balanceOf(alice, daysBeforeToday(2)))
        assertEquals(Money(1250, eur), balanceOf(alice, yesterday))
        assertEquals(Money(1250, eur), balanceOf(alice))
    }

    @Test
    fun `counts a backdated entry from the day it happened, not the day it was recorded`() {
        record(alice, MovementType.DEPOSIT, 1000, daysBeforeToday(7))

        assertEquals(Money.zero(eur), balanceOf(alice, daysBeforeToday(8)))
        assertEquals(Money(1000, eur), balanceOf(alice, daysBeforeToday(7)))
    }

    @Test
    fun `says which date each balance was read as of`() {
        assertEquals(TODAY, calculator.calculate(BalanceQuery(TODAY, alice.id), Cursor(50)).items.single().onDate)
    }

    @Test
    fun `keeps one holder's movements out of another's balance`() {
        record(alice, MovementType.DEPOSIT, 1000)
        record(bob, MovementType.DEPOSIT, 250)

        assertEquals(Money(1000, eur), balanceOf(alice))
        assertEquals(Money(250, eur), balanceOf(bob))
    }

    @Test
    fun `answers for every account it keeps when none is named`() {
        record(alice, MovementType.DEPOSIT, 1000)
        record(bob, MovementType.DEPOSIT, 250)

        val balances = calculator.calculate(BalanceQuery(TODAY), Cursor(50))

        assertEquals(
            setOf("Alice", "Bob", "Cash EUR"),
            balances.items.map { it.account.name }.toSet(),
        )
        assertEquals(Money(1250, eur), balances.items.single { it.account.name == "Cash EUR" }.amount)
        assertNull(balances.nextCursor)
    }

    @Test
    fun `has nothing to say about an account it does not keep`() {
        val ghost = factory.forHolder("Ghost", eur, AccountReference("ACC-Ghost".uppercase()))

        assertEquals(emptyList(), calculator.calculate(BalanceQuery(TODAY, ghost.id), Cursor(50)).items)
    }

    @Test
    fun `what the ledger holds equals what it owes`() {
        record(alice, MovementType.DEPOSIT, 1000)
        record(bob, MovementType.DEPOSIT, 250)
        record(alice, MovementType.WITHDRAWAL, 400)

        val cash = accounts.byReference(AccountRole.CASH.referenceFor(eur))!!

        assertEquals(Money(850, eur), balanceOf(cash))
        assertEquals(Money(850, eur), balanceOf(alice) + balanceOf(bob))
    }
}
