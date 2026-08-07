package altak.ledger.application.ledger

import altak.ledger.CountingTransactionManager
import altak.ledger.application.account.AccountNotFound
import altak.ledger.application.ledger.service.ViewHistory
import altak.ledger.application.shared.CursorDto
import altak.ledger.application.ledger.service.ViewHistoryService
import altak.ledger.domain.Cursor
import altak.ledger.domain.Money
import altak.ledger.domain.account.Account
import altak.ledger.domain.ledger.Direction
import altak.ledger.domain.ledger.EntryId
import altak.ledger.domain.ledger.EntryLine
import altak.ledger.domain.ledger.JournalEntry
import altak.ledger.fixedClock
import altak.ledger.infrastructure.persistence.InMemoryAccountRepository
import altak.ledger.infrastructure.persistence.InMemoryJournalEntryRepository
import java.util.Currency
import kotlin.uuid.Uuid
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ViewHistoryServiceTest {

    private val eur = Currency.getInstance("EUR")
    private val clock = fixedClock()
    private val accounts = InMemoryAccountRepository()
    private val entries = InMemoryJournalEntryRepository()
    private val transactions = CountingTransactionManager()
    private val service = ViewHistoryService(accounts, entries, transactions)

    private val alice = Account.forHolder("Alice", eur, clock).also(accounts::save)
    private val bob = Account.forHolder("Bob", eur, clock).also(accounts::save)
    private val cash = Account.forCash(eur, clock).also(accounts::save)

    private fun movementOf(holder: Account, minorUnits: Long) =
        JournalEntry(
            "Deposit",
            listOf(
                EntryLine(holder.id, Direction.CREDIT, Money(minorUnits, eur)),
                EntryLine(cash.id, Direction.DEBIT, Money(minorUnits, eur)),
            ),
            clock,
        ).also(entries::save)

    private fun history(after: Uuid? = null, limit: Int = 50) =
        service.execute(ViewHistory(alice.id.toString(), CursorDto(after, limit)))

    @Test
    fun `has nothing to show for an account that never moved`() {
        val history = history()

        assertEquals(emptyList(), history.entries)
        assertNull(history.nextCursor)
    }

    @Test
    fun `shows both sides of every movement it lists`() {
        movementOf(alice, 1000)

        val entry = history().entries.single()

        assertEquals("Deposit", entry.description)
        assertEquals(BigDecimal("10.00"), entry.lines.single { it.accountId == alice.id.toString() }.amount)
        assertEquals("CREDIT", entry.lines.single { it.accountId == alice.id.toString() }.direction)
        assertEquals("DEBIT", entry.lines.single { it.accountId == cash.id.toString() }.direction)
    }

    @Test
    fun `lists movements in the order they were recorded`() {
        val movements = (1..3).map { movementOf(alice, it * 100L) }

        assertEquals(movements.map { it.id.toString() }, history().entries.map { it.id })
    }

    @Test
    fun `keeps another holder's movements out`() {
        val hers = movementOf(alice, 1000)
        movementOf(bob, 500)

        assertEquals(listOf(hers.id.toString()), history().entries.map { it.id })
    }

    @Test
    fun `walks the history a page at a time`() {
        val movements = (1..5).map { movementOf(alice, it * 100L).id }

        val firstPage = history(limit = 2)
        val secondPage = history(after = Uuid.parse(firstPage.nextCursor!!), limit = 2)
        val lastPage = history(after = Uuid.parse(secondPage.nextCursor!!), limit = 2)

        assertEquals(movements.take(2).map { it.toString() }, firstPage.entries.map { it.id })
        assertEquals(movements.drop(2).take(2).map { it.toString() }, secondPage.entries.map { it.id })
        assertEquals(movements.drop(4).map { it.toString() }, lastPage.entries.map { it.id })
        assertEquals(movements[1].toString(), firstPage.nextCursor)
        assertNull(lastPage.nextCursor)
    }

    @Test
    fun `refuses a page the ledger would not hand out`() {
        assertFailsWith<Cursor.InvalidLimit> { history(limit = 0) }
        assertFailsWith<Cursor.InvalidLimit> { history(limit = Cursor.MAX_LIMIT + 1) }
    }

    @Test
    fun `refuses a cursor that is not an id this ledger issued`() {
        assertFailsWith<EntryId.Malformed> { history(after = Uuid.random()) }
    }

    @Test
    fun `refuses an id that names no account`() {
        assertFailsWith<AccountNotFound> { service.execute(ViewHistory("not-an-account")) }
    }

    @Test
    fun `runs in one transaction`() {
        history()

        assertEquals(1, transactions.transactions)
    }
}
