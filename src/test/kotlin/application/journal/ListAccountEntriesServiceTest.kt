package altak.ledger.application.journal

import altak.ledger.CountingTransactionManager
import altak.ledger.application.account.AccountNotFound
import altak.ledger.application.journal.EntryQueryDto
import altak.ledger.application.journal.service.ListAccountEntries
import altak.ledger.application.shared.CursorDto
import altak.ledger.application.journal.service.ListAccountEntriesService
import altak.ledger.domain.Cursor
import altak.ledger.domain.Money
import altak.ledger.domain.account.Account
import altak.ledger.domain.account.AccountReference
import altak.ledger.domain.account.AccountRole
import altak.ledger.domain.journal.Direction
import altak.ledger.domain.journal.EntryId
import altak.ledger.domain.journal.EntryLine
import altak.ledger.domain.journal.JournalEntry
import altak.ledger.fixedClock
import altak.ledger.ids
import altak.ledger.infrastructure.persistence.InMemoryAccountRepository
import altak.ledger.infrastructure.persistence.InMemoryJournalEntryRepository
import java.util.Currency
import kotlin.uuid.Uuid
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ListAccountEntriesServiceTest {

    private val eur = Currency.getInstance("EUR")
    private val clock = fixedClock()
    private val accounts = InMemoryAccountRepository()
    private val entries = InMemoryJournalEntryRepository()
    private val transactions = CountingTransactionManager()
    private val service = ListAccountEntriesService(accounts, entries, transactions)

    private val alice = Account.forHolder("Alice", eur, ids, clock, AccountReference("ACC-Alice".uppercase())).also(accounts::save)
    private val bob = Account.forHolder("Bob", eur, ids, clock, AccountReference("ACC-Bob".uppercase())).also(accounts::save)
    private val cash = Account.internal(AccountRole.CASH, eur, ids, clock).also(accounts::save)

    private fun movementOf(holder: Account, minorUnits: Long) =
        JournalEntry(
            "Deposit",
            listOf(
                EntryLine(holder.id, Direction.CREDIT, Money(minorUnits, eur)),
                EntryLine(cash.id, Direction.DEBIT, Money(minorUnits, eur)),
            ),
            ids,
            clock,
        ).also(entries::save)

    private fun history(after: Uuid? = null, limit: Int = 50) =
        service.execute(ListAccountEntries(EntryQueryDto(alice.id.toString()), CursorDto(after, limit)))

    @Test
    fun `has nothing to show for an account that never moved`() {
        val history = history()

        assertEquals(emptyList(), history.items)
        assertNull(history.nextCursor)
    }

    @Test
    fun `shows both sides of every movement it lists`() {
        movementOf(alice, 1000)

        val entry = history().items.single()

        assertEquals("Deposit", entry.description)
        assertEquals(BigDecimal("10.00"), entry.lines.single { it.accountId == alice.id.toString() }.amount)
        assertEquals("CREDIT", entry.lines.single { it.accountId == alice.id.toString() }.direction)
        assertEquals("DEBIT", entry.lines.single { it.accountId == cash.id.toString() }.direction)
    }

    @Test
    fun `lists movements in the order they were recorded`() {
        val movements = (1..3).map { movementOf(alice, it * 100L) }

        assertEquals(movements.map { it.id.toString() }, history().items.map { it.id.toString() })
    }

    @Test
    fun `keeps another holder's movements out`() {
        val hers = movementOf(alice, 1000)
        movementOf(bob, 500)

        assertEquals(listOf(hers.id.toString()), history().items.map { it.id.toString() })
    }

    @Test
    fun `walks the history a page at a time`() {
        val movements = (1..5).map { movementOf(alice, it * 100L).id }

        val firstPage = history(limit = 2)
        val secondPage = history(after = Uuid.parse(firstPage.nextCursor!!), limit = 2)
        val lastPage = history(after = Uuid.parse(secondPage.nextCursor!!), limit = 2)

        assertEquals(movements.take(2).map { it.toString() }, firstPage.items.map { it.id.toString() })
        assertEquals(movements.drop(2).take(2).map { it.toString() }, secondPage.items.map { it.id.toString() })
        assertEquals(movements.drop(4).map { it.toString() }, lastPage.items.map { it.id.toString() })
        assertEquals(movements[1].toString(), firstPage.nextCursor)
        assertNull(lastPage.nextCursor)
    }

    @Test
    fun `refuses a page the ledger would not hand out`() {
        assertFailsWith<Cursor.InvalidLimit> { history(limit = 0) }
        assertFailsWith<Cursor.InvalidLimit> { history(limit = Cursor.MAX_LIMIT + 1) }
    }

    @Test
    fun `has nothing to show past a cursor it does not know`() {
        movementOf(alice, 1000)

        assertEquals(emptyList(), history(after = Uuid.random()).items)
    }

    @Test
    fun `refuses an id that names no account`() {
        assertFailsWith<AccountNotFound> { service.execute(ListAccountEntries(EntryQueryDto("not-an-account"), CursorDto())) }
    }

    @Test
    fun `runs in one transaction`() {
        history()

        assertEquals(1, transactions.transactions)
    }
}
