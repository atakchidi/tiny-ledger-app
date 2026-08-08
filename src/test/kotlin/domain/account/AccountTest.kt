package altak.ledger.domain.account

import altak.ledger.NOW
import altak.ledger.domain.Money
import altak.ledger.domain.journal.Direction
import altak.ledger.domain.journal.MovementType
import altak.ledger.accountFactory
import altak.ledger.fixedClock
import java.util.Currency
import kotlin.test.Test
import kotlin.test.assertEquals

class AccountTest {

    private val eur = Currency.getInstance("EUR")
    private val clock = fixedClock()
    private val factory = accountFactory(clock)

    private val chart = ChartOfAccounts { role, currency -> factory.internal(role, currency) }

    private val alice = factory.forHolder("Alice", eur, AccountReference("ACC-Alice".uppercase()))
    private val cash = factory.internal(AccountRole.CASH, eur)

    private fun Account.moving(movement: MovementType, minorUnits: Long, currency: Currency = eur) =
        move(movement, Money(minorUnits, currency), chart, clock)

    @Test
    fun `takes its creation time and a version 7 id from the clock`() {
        assertEquals(NOW, alice.createdAt)
        assertEquals('7', alice.id.toString()[14])
    }

    @Test
    fun `keeps the reference the holder brought`() {
        val named = factory.forHolder("Alice", eur, AccountReference("ACC-000123"))

        assertEquals("ACC-000123", named.reference.toString())
    }

    @Test
    fun `takes its name, type and reference from the role it serves`() {
        assertEquals("CASH-EUR", cash.reference.toString())
        assertEquals("Cash EUR", cash.name)
        assertEquals(AccountType.ASSET, cash.type)
    }

    @Test
    fun `has not been touched since it was opened`() {
        assertEquals(alice.createdAt, alice.updatedAt)
    }


    @Test
    fun `grows on its own normal side and shrinks on the other`() {
        assertEquals(Direction.CREDIT, alice.moving(MovementType.DEPOSIT, 1000).lines.first().direction)
        assertEquals(Direction.DEBIT, alice.moving(MovementType.WITHDRAWAL, 1000).lines.first().direction)
        assertEquals(Direction.DEBIT, cash.moving(MovementType.DEPOSIT, 1000).lines.first().direction)
        assertEquals(Direction.CREDIT, cash.moving(MovementType.WITHDRAWAL, 1000).lines.first().direction)
    }

    @Test
    fun `settles a movement against the account the role it names stands for`() {
        val deposit = alice.moving(MovementType.DEPOSIT, 1000)

        assertEquals(listOf("ACC-ALICE", "CASH-EUR"), deposit.accounts.map { it.reference.toString() })
        assertEquals(deposit.accounts.map { it.id }, deposit.lines.map { it.accountId })
        assertEquals(listOf(Direction.CREDIT, Direction.DEBIT), deposit.lines.map { it.direction })
        assertEquals(listOf(Money(1000, eur), Money(1000, eur)), deposit.lines.map { it.amount })
    }

    @Test
    fun `carries each side onto the balance of the account it lands on`() {
        val deposit = alice.moving(MovementType.DEPOSIT, 1000)

        assertEquals(Money(1000, eur), deposit.accounts.first().balance)
        assertEquals(Money(1000, eur), deposit.accounts.last().balance)
        assertEquals(NOW, deposit.accounts.first().updatedAt)
    }

    @Test
    fun `takes each balance from the account it belongs to, not from the other side`() {
        val withdrawal = alice.copy(balance = Money(1000, eur)).moving(MovementType.WITHDRAWAL, 400)

        assertEquals(Money(600, eur), withdrawal.accounts.first().balance)
        assertEquals(Money(-400, eur), withdrawal.accounts.last().balance)
    }

}
