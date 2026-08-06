package altak.ledger.domain

import altak.ledger.Faker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.uuid.Uuid

class LedgerTest {

    private val eur = currencyOf("EUR")
    private val usd = currencyOf("USD")
    private val clock = Faker.clock()

    private fun ledger() = Ledger(clock)

    private fun Ledger.alice() = open("Alice", eur)

    private fun Ledger.totalFor(type: AccountType) =
        accounts.filter { it.type == type && it.currency == eur }
            .fold(Money.zero(eur)) { running, account -> running + balanceOf(account.id) }

    @Test
    fun `an account starts with a zero balance`() {
        val ledger = ledger()
        val alice = ledger.alice()

        assertEquals(Money.zero(eur), ledger.balanceOf(alice.id))
        assertEquals(listOf(alice), ledger.accounts)
    }

    @Test
    fun `an account holder's money is a liability of the ledger`() {
        val ledger = ledger()
        val alice = ledger.alice()

        assertEquals(AccountType.LIABILITY, alice.type)
    }

    @Test
    fun `a deposit raises the balance and the cash the ledger holds`() {
        val ledger = ledger()
        val alice = ledger.alice()

        ledger.deposit(alice.id, Money(1000, eur))
        val cash = ledger.accounts.single { it.type == AccountType.ASSET }

        assertEquals(Money(1000, eur), ledger.balanceOf(alice.id))
        assertEquals(Money(1000, eur), ledger.balanceOf(cash.id))
        assertEquals("Cash EUR", cash.name)
    }

    @Test
    fun `a withdrawal lowers the balance`() {
        val ledger = ledger()
        val alice = ledger.alice()

        ledger.deposit(alice.id, Money(1000, eur))
        ledger.withdraw(alice.id, Money(400, eur))

        assertEquals(Money(600, eur), ledger.balanceOf(alice.id))
    }

    @Test
    fun `a withdrawal may take the balance below zero`() {
        val ledger = ledger()
        val alice = ledger.alice()

        ledger.withdraw(alice.id, Money(250, eur))

        assertEquals(Money(-250, eur), ledger.balanceOf(alice.id))
    }

    @Test
    fun `what the ledger holds always equals what it owes`() {
        val ledger = ledger()
        val alice = ledger.alice()
        val bob = ledger.open("Bob", eur)

        ledger.deposit(alice.id, Money(1000, eur))
        ledger.deposit(bob.id, Money(250, eur))
        ledger.withdraw(alice.id, Money(400, eur))

        assertEquals(Money(850, eur), ledger.totalFor(AccountType.ASSET))
        assertEquals(ledger.totalFor(AccountType.ASSET), ledger.totalFor(AccountType.LIABILITY))
    }

    @Test
    fun `each currency gets its own cash account`() {
        val ledger = ledger()
        val euros = ledger.alice()
        val dollars = ledger.open("Dollars", usd)

        ledger.deposit(euros.id, Money(100, eur))
        ledger.deposit(dollars.id, Money(100, usd))

        assertEquals(2, ledger.accounts.count { it.type == AccountType.ASSET })
        assertEquals(Money(100, usd), ledger.balanceOf(dollars.id))
    }

    @Test
    fun `history lists the entries touching the account in the order they were recorded`() {
        val ledger = ledger()
        val alice = ledger.alice()
        val bob = ledger.open("Bob", eur)

        val deposit = ledger.deposit(alice.id, Money(1000, eur))
        ledger.deposit(bob.id, Money(500, eur))
        val withdrawal = ledger.withdraw(alice.id, Money(200, eur))

        assertEquals(listOf(deposit, withdrawal), ledger.historyOf(alice.id))
    }

    @Test
    fun `an unknown account cannot be used`() {
        val ledger = ledger()
        val unknown = AccountId(Uuid.generateV7NonMonotonicAt(clock.now()))

        assertFailsWith<LedgerException.AccountNotFound> { ledger.balanceOf(unknown) }
        assertFailsWith<LedgerException.AccountNotFound> { ledger.historyOf(unknown) }
        assertFailsWith<LedgerException.AccountNotFound> { ledger.deposit(unknown, Money(100, eur)) }
    }

    @Test
    fun `an account cannot take a movement in another currency`() {
        val ledger = ledger()
        val alice = ledger.alice()

        assertFailsWith<LedgerException.CurrencyMismatch> { ledger.deposit(alice.id, Money(100, usd)) }
    }

    @Test
    fun `is rebuilt from the accounts and entries it is given`() {
        val existing = Ledger(clock)
        val alice = existing.alice()
        existing.deposit(alice.id, Money(1000, eur))

        val rebuilt = Ledger(clock, existing.accounts, existing.historyOf(alice.id))

        assertEquals(Money(1000, eur), rebuilt.balanceOf(alice.id))
    }

    @Test
    fun `an entry keeps debits and credits equal`() {
        val ledger = ledger()
        val alice = ledger.alice()
        val bob = ledger.open("Bob", eur)

        assertFailsWith<LedgerException.UnbalancedEntry> {
            JournalEntry(
                "lopsided",
                listOf(
                    EntryLine(alice.id, Direction.CREDIT, Money(1000, eur)),
                    EntryLine(bob.id, Direction.DEBIT, Money(900, eur)),
                ),
                clock,
            )
        }
    }

    @Test
    fun `an entry needs at least two lines`() {
        val ledger = ledger()
        val alice = ledger.alice()

        assertFailsWith<LedgerException.MalformedEntry> {
            JournalEntry("lonely", listOf(EntryLine(alice.id, Direction.CREDIT, Money(1000, eur))), clock)
        }
    }

    @Test
    fun `an entry cannot mix currencies`() {
        val ledger = ledger()
        val euros = ledger.alice()
        val dollars = ledger.open("Dollars", usd)

        assertFailsWith<LedgerException.CurrencyMismatch> {
            JournalEntry(
                "mixed",
                listOf(
                    EntryLine(euros.id, Direction.CREDIT, Money(1000, eur)),
                    EntryLine(dollars.id, Direction.DEBIT, Money(1000, usd)),
                ),
                clock,
            )
        }
    }
}
