package altak.ledger.domain

import altak.ledger.Faker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AccountTest {

    private val eur = currencyOf("EUR")
    private val usd = currencyOf("USD")
    private val clock = Faker.clock()

    private val alice = Account("Alice", eur, AccountType.LIABILITY, clock)
    private val cash = Account("Cash EUR", eur, AccountType.ASSET, clock)

    private fun JournalEntry.directionOf(account: Account) = lines.single { it.accountId == account.id }.direction

    @Test
    fun `takes its creation time and a version 7 id from the clock`() {
        assertEquals(Faker.NOW, alice.createdAt)
        assertEquals('7', alice.id.toString()[14])
    }

    @Test
    fun `a deposit increases both sides of the books`() {
        val entry = alice.deposit(Money(1000, eur), cash, clock)

        assertEquals(Direction.CREDIT, entry.directionOf(alice))
        assertEquals(Direction.DEBIT, entry.directionOf(cash))
        assertEquals("Deposit", entry.description)
        assertEquals(Faker.NOW, entry.occurredAt)
    }

    @Test
    fun `a withdrawal decreases both sides of the books`() {
        val entry = alice.withdraw(Money(400, eur), cash, clock)

        assertEquals(Direction.DEBIT, entry.directionOf(alice))
        assertEquals(Direction.CREDIT, entry.directionOf(cash))
        assertEquals("Withdrawal", entry.description)
    }

    @Test
    fun `carries a description of the movement`() {
        val entry = alice.deposit(Money(1000, eur), cash, clock, "Salary")

        assertEquals("Salary", entry.description)
    }

    @Test
    fun `a line counts towards the balance in the direction it faces`() {
        val credited = EntryLine(alice.id, Direction.CREDIT, Money(1000, eur))
        val debited = EntryLine(alice.id, Direction.DEBIT, Money(1000, eur))

        assertEquals(Money(1000, eur), credited.signedAgainst(alice.type.normalSide))
        assertEquals(Money(-1000, eur), debited.signedAgainst(alice.type.normalSide))
    }

    @Test
    fun `refuses an amount in another currency`() {
        assertFailsWith<LedgerException.CurrencyMismatch> { alice.deposit(Money(100, usd), cash, clock) }
        assertFailsWith<LedgerException.CurrencyMismatch> { alice.withdraw(Money(100, usd), cash, clock) }
    }

    @Test
    fun `refuses a non-positive amount`() {
        assertFailsWith<LedgerException.MalformedEntry> { alice.deposit(Money(0, eur), cash, clock) }
        assertFailsWith<LedgerException.MalformedEntry> { alice.withdraw(Money(-1, eur), cash, clock) }
    }
}
