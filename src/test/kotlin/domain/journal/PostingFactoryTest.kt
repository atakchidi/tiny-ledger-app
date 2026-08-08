package altak.ledger.domain.journal

import altak.ledger.NOW
import altak.ledger.domain.Money
import altak.ledger.domain.account.Account
import altak.ledger.domain.account.AccountReference
import altak.ledger.domain.account.AccountRole
import altak.ledger.domain.account.AccountType
import altak.ledger.domain.account.ChartOfAccounts
import altak.ledger.accountFactory
import altak.ledger.fixedClock
import altak.ledger.journalEntryFactory
import java.util.Currency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PostingFactoryTest {

    private val eur = Currency.getInstance("EUR")
    private val usd = Currency.getInstance("USD")
    private val clock = fixedClock()
    private val factory = accountFactory(clock)

    private val chart = ChartOfAccounts { role, currency -> factory.internal(role, currency) }
    private val postings = PostingFactory(chart, journalEntryFactory(clock), clock)

    private val alice = factory.forHolder("Alice", eur, AccountReference("ACC-Alice".uppercase()))

    private fun Posting.of(reference: String) = accounts.single { it.reference.toString() == reference }

    private fun Posting.sideOf(reference: String) =
        entry.lines.single { it.accountId == of(reference).id }.direction

    private fun Posting.amountOf(reference: String) =
        entry.lines.single { it.accountId == of(reference).id }.amount

    @Test
    fun `a deposit credits the holder and debits the cash behind them`() {
        val posting = postings.create(alice, MovementType.DEPOSIT, Money(1000, eur))

        assertEquals(Direction.CREDIT, posting.sideOf(alice.reference.toString()))
        assertEquals(Direction.DEBIT, posting.sideOf("CASH-EUR"))
        assertEquals(Money(1000, eur), posting.amountOf(alice.reference.toString()))
        assertEquals(Money(1000, eur), posting.amountOf("CASH-EUR"))
    }

    @Test
    fun `a withdrawal turns both sides around`() {
        val posting = postings.create(alice, MovementType.WITHDRAWAL, Money(400, eur))

        assertEquals(Direction.DEBIT, posting.sideOf(alice.reference.toString()))
        assertEquals(Direction.CREDIT, posting.sideOf("CASH-EUR"))
        assertEquals(Money(400, eur), posting.amountOf(alice.reference.toString()))
        assertEquals(Money(400, eur), posting.amountOf("CASH-EUR"))
    }

    @Test
    fun `every movement lands the two sides on opposite sides of the entry`() {
        MovementType.entries.forEach { movement ->
            val lines = postings.create(alice, movement, Money(1000, eur)).entry.lines

            assertEquals(2, lines.size)
            assertEquals(setOf(Direction.DEBIT, Direction.CREDIT), lines.map { it.direction }.toSet())
        }
    }

    @Test
    fun `a counterpart of any category takes the side opposite the subject`() {
        AccountType.entries.forEach { type ->
            val counterpart = factory.internal(AccountRole.CASH, eur).copy(type = type)
            val posting = PostingFactory({ _, _ -> counterpart }, journalEntryFactory(clock), clock)
                .create(alice, MovementType.DEPOSIT, Money(1000, eur))

            val subjectSide = posting.entry.lines.single { it.accountId == alice.id }.direction
            val counterpartSide = posting.entry.lines.single { it.accountId == counterpart.id }.direction

            assertEquals(subjectSide.opposite, counterpartSide, "for $type")
        }
    }

    @Test
    fun `names the movement unless the caller says otherwise`() {
        assertEquals("Deposit", postings.create(alice, MovementType.DEPOSIT, Money(1000, eur)).entry.description)
        assertEquals(
            "Salary",
            postings.create(alice, MovementType.DEPOSIT, Money(1000, eur), "Salary").entry.description,
        )
    }

    @Test
    fun `takes the time of the posting from the clock`() {
        assertEquals(NOW, postings.create(alice, MovementType.DEPOSIT, Money(1000, eur)).entry.createdAt)
    }

    @Test
    fun `settles against cash in the account's own currency`() {
        val dollars = factory.forHolder("Dollars", usd, AccountReference("ACC-Dollars".uppercase()))

        val posting = postings.create(dollars, MovementType.DEPOSIT, Money(100, usd))

        assertEquals(usd, posting.of("CASH-USD").currency)
        assertFailsWith<Account.CurrencyMismatch> { postings.create(dollars, MovementType.DEPOSIT, Money(100, eur)) }
    }

    @Test
    fun `refuses an amount of nothing`() {
        assertFailsWith<EntryLine.NonPositiveAmount> { postings.create(alice, MovementType.DEPOSIT, Money(0, eur)) }
    }
}
