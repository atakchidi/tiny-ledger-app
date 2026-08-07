package altak.ledger.domain.entry

import altak.ledger.NOW
import altak.ledger.domain.Cursor
import altak.ledger.domain.Money
import altak.ledger.domain.account.Account
import altak.ledger.domain.account.AccountRole
import altak.ledger.fixedClock
import altak.ledger.infrastructure.persistence.InMemoryAccountRepository
import altak.ledger.infrastructure.persistence.InMemoryJournalEntryRepository
import altak.ledger.infrastructure.persistence.RepositoryChartOfAccounts
import altak.ledger.infrastructure.persistence.RepositoryPostingStore
import java.util.Currency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class BalancesCalculatorTest {

    private val eur = Currency.getInstance("EUR")
    private val accounts = InMemoryAccountRepository()
    private val entries = InMemoryJournalEntryRepository()
    private val store = RepositoryPostingStore(accounts, entries)
    private val calculator = BalancesCalculator(accounts, entries)

    private val alice = Account.forHolder("Alice", eur, fixedClock()).also(accounts::save)
    private val bob = Account.forHolder("Bob", eur, fixedClock()).also(accounts::save)

    private fun record(account: Account, movement: MovementType, minorUnits: Long, at: Instant) {
        val clock = fixedClock(at)
        val chart = RepositoryChartOfAccounts(accounts, clock)

        store.store(PostingFactory(chart, clock).create(account, movement, Money(minorUnits, eur)))
    }

    private fun balanceOf(account: Account, onDate: Instant = NOW) =
        calculator.calculate(BalanceQuery(onDate, account.id), Cursor()).items.single().amount

    @Test
    fun `an account with nothing in the journal owes nothing`() {
        assertEquals(Money.zero(eur), balanceOf(alice))
    }

    @Test
    fun `folds every line the journal holds for the account`() {
        record(alice, MovementType.DEPOSIT, 1000, NOW)
        record(alice, MovementType.WITHDRAWAL, 400, NOW)

        assertEquals(Money(600, eur), balanceOf(alice))
    }

    @Test
    fun `reads the journal as it stood on the date it is asked for`() {
        val yesterday = NOW.minus(1.days)
        val tomorrow = NOW.plus(1.days)

        record(alice, MovementType.DEPOSIT, 1000, yesterday)
        record(alice, MovementType.DEPOSIT, 250, tomorrow)

        assertEquals(Money.zero(eur), balanceOf(alice, yesterday.minus(1.days)))
        assertEquals(Money(1000, eur), balanceOf(alice, yesterday))
        assertEquals(Money(1000, eur), balanceOf(alice, NOW))
        assertEquals(Money(1250, eur), balanceOf(alice, tomorrow))
    }

    @Test
    fun `says which moment each balance was read at`() {
        assertEquals(NOW, calculator.calculate(BalanceQuery(NOW, alice.id), Cursor()).items.single().onDate)
    }

    @Test
    fun `keeps one holder's movements out of another's balance`() {
        record(alice, MovementType.DEPOSIT, 1000, NOW)
        record(bob, MovementType.DEPOSIT, 250, NOW)

        assertEquals(Money(1000, eur), balanceOf(alice))
        assertEquals(Money(250, eur), balanceOf(bob))
    }

    @Test
    fun `answers for every account it keeps when none is named`() {
        record(alice, MovementType.DEPOSIT, 1000, NOW)
        record(bob, MovementType.DEPOSIT, 250, NOW)

        val balances = calculator.calculate(BalanceQuery(NOW), Cursor())

        assertEquals(
            setOf("Alice", "Bob", "Cash EUR"),
            balances.items.map { it.account.name }.toSet(),
        )
        assertEquals(Money(1250, eur), balances.items.single { it.account.name == "Cash EUR" }.amount)
        assertNull(balances.nextCursor)
    }

    @Test
    fun `hands back the accounts a page at a time`() {
        record(alice, MovementType.DEPOSIT, 1000, NOW)

        val firstPage = calculator.calculate(BalanceQuery(NOW), Cursor(limit = 2))

        assertEquals(2, firstPage.items.size)
        assertEquals(firstPage.items.last().account.id.toString(), firstPage.nextCursor)
    }

    @Test
    fun `has nothing to say about an account it does not keep`() {
        val ghost = Account.forHolder("Ghost", eur, fixedClock())

        assertEquals(emptyList(), calculator.calculate(BalanceQuery(NOW, ghost.id), Cursor()).items)
    }

    @Test
    fun `what the ledger holds equals what it owes`() {
        record(alice, MovementType.DEPOSIT, 1000, NOW)
        record(bob, MovementType.DEPOSIT, 250, NOW)
        record(alice, MovementType.WITHDRAWAL, 400, NOW)

        val cash = accounts.byReference(AccountRole.CASH.referenceFor(eur))!!

        assertEquals(Money(850, eur), balanceOf(cash))
        assertEquals(Money(850, eur), balanceOf(alice) + balanceOf(bob))
    }
}
