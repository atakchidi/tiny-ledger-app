package altak.ledger.infrastructure.persistence

import altak.ledger.TODAY
import altak.ledger.accountFactory
import altak.ledger.advancingClock
import altak.ledger.domain.Cursor
import altak.ledger.domain.Money
import altak.ledger.domain.account.Account
import altak.ledger.domain.account.AccountReference
import altak.ledger.domain.account.AccountRole
import altak.ledger.domain.journal.Direction
import altak.ledger.domain.journal.EntryLine
import altak.ledger.fixedClock
import altak.ledger.journalEntryFactory
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import java.util.Currency
import kotlin.test.Test
import kotlin.test.assertEquals

class InMemoryJournalEntryRepositoryTest {

    private val eur = Currency.getInstance("EUR")
    private val factory = accountFactory(fixedClock())
    private val journal = journalEntryFactory(advancingClock())
    private val repository = InMemoryJournalEntryRepository()

    private val alice = factory.forHolder("Alice", eur, AccountReference("ACC-ALICE"))
    private val cash = factory.internal(AccountRole.CASH, eur)
    private val bob = factory.forHolder("Bob", eur, AccountReference("ACC-BOB"))

    private fun movementOf(holder: Account, minorUnits: Long, occurredOn: LocalDate? = null) =
        journal.create(
            "movement",
            listOf(
                EntryLine(holder.id, Direction.CREDIT, Money(minorUnits, eur)),
                EntryLine(cash.id, Direction.DEBIT, Money(minorUnits, eur)),
            ),
            occurredOn,
        ).also(repository::save)

    @Test
    fun `hands back nothing for an account with no entries`() {
        assertEquals(emptyList(), repository.byAccount(alice.id, Cursor(50)).items)
        assertEquals(emptyList(), repository.linesOf(alice.id, TODAY))
    }

    @Test
    fun `finds an entry under every account it touches`() {
        val entry = movementOf(alice, 1000)

        assertEquals(listOf(entry), repository.byAccount(alice.id, Cursor(50)).items)
        assertEquals(listOf(entry), repository.byAccount(cash.id, Cursor(50)).items)
    }

    @Test
    fun `lists every entry once, however many accounts it touches`() {
        val hers = movementOf(alice, 1000)
        val his = movementOf(bob, 500)

        assertEquals(listOf(hers, his), repository.all(Cursor(50)).items)
    }

    @Test
    fun `keeps entries of other accounts out`() {
        val hers = movementOf(alice, 1000)
        val his = movementOf(bob, 500)

        assertEquals(listOf(hers), repository.byAccount(alice.id, Cursor(50)).items)
        assertEquals(listOf(his), repository.byAccount(bob.id, Cursor(50)).items)
        assertEquals(listOf(hers, his), repository.byAccount(cash.id, Cursor(50)).items)
    }

    @Test
    fun `reads only the lines that land on the account it is asked about`() {
        movementOf(alice, 1000)
        movementOf(bob, 500)

        assertEquals(listOf(Money(1000, eur)), repository.linesOf(alice.id, TODAY).map { it.amount })
        assertEquals(
            listOf(Money(1000, eur), Money(500, eur)),
            repository.linesOf(cash.id, TODAY).map { it.amount },
        )
    }

    @Test
    fun `leaves out the lines of entries that happened after the date it is asked for`() {
        val lastWeek = TODAY.minus(7, DateTimeUnit.DAY)
        movementOf(alice, 1000, lastWeek)
        movementOf(alice, 400)

        assertEquals(emptyList(), repository.linesOf(alice.id, lastWeek.minus(1, DateTimeUnit.DAY)))
        assertEquals(listOf(Money(1000, eur)), repository.linesOf(alice.id, lastWeek).map { it.amount })
        assertEquals(2, repository.linesOf(alice.id, TODAY).size)
    }
}
