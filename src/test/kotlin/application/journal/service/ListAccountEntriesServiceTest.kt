package altak.ledger.application.journal.service

import altak.ledger.CountingTransactionManager
import altak.ledger.accountFactory
import altak.ledger.advancingClock
import altak.ledger.application.journal.SearchAccountEntriesDto
import altak.ledger.application.shared.CursorDto
import altak.ledger.domain.Money
import altak.ledger.domain.account.Account
import altak.ledger.domain.account.AccountReference
import altak.ledger.domain.account.AccountRole
import altak.ledger.domain.journal.Direction
import altak.ledger.domain.journal.EntryLine
import altak.ledger.fixedClock
import altak.ledger.infrastructure.persistence.InMemoryAccountRepository
import altak.ledger.infrastructure.persistence.InMemoryJournalEntryRepository
import altak.ledger.journalEntryFactory
import java.util.Currency
import kotlin.test.Test
import kotlin.test.assertEquals

class ListAccountEntriesServiceTest {

    private val eur = Currency.getInstance("EUR")
    private val factory = accountFactory(fixedClock())
    private val journal = journalEntryFactory(advancingClock())
    private val accounts = InMemoryAccountRepository()
    private val entries = InMemoryJournalEntryRepository()
    private val transactions = CountingTransactionManager()
    private val service = ListAccountEntriesService(accounts, entries, transactions)

    private val alice = factory.forHolder("Alice", eur, AccountReference("ACC-ALICE")).also(accounts::save)
    private val bob = factory.forHolder("Bob", eur, AccountReference("ACC-BOB")).also(accounts::save)
    private val cash = factory.internal(AccountRole.CASH, eur).also(accounts::save)

    private fun movementOf(holder: Account, minorUnits: Long) =
        journal.create(
            "Deposit",
            listOf(
                EntryLine(holder.id, Direction.CREDIT, Money(minorUnits, eur)),
                EntryLine(cash.id, Direction.DEBIT, Money(minorUnits, eur)),
            ),
        ).also(entries::save)

    private fun history() =
        service.execute(ListAccountEntries(SearchAccountEntriesDto(alice.id.toString()), CursorDto()))

    private fun everything() =
        service.execute(ListAccountEntries(SearchAccountEntriesDto(), CursorDto()))

    @Test
    fun `has nothing to show for an account that never moved`() {
        assertEquals(emptyList(), history().items)
    }

    @Test
    fun `names the account each side of an entry lands on`() {
        movementOf(alice, 1000)

        val entry = history().items.single()

        assertEquals("Deposit", entry.description)
        assertEquals(eur, entry.currency)
        with(entry.lines.single { it.accountId == alice.id.toString() }) {
            assertEquals("ACC-ALICE", reference)
            assertEquals("CREDIT", direction)
            assertEquals("10.00", amount)
        }
        with(entry.lines.single { it.accountId == cash.id.toString() }) {
            assertEquals("CASH-EUR", reference)
            assertEquals("DEBIT", direction)
        }
    }

    @Test
    fun `shows one holder's history, or the whole journal when no account is named`() {
        val hers = movementOf(alice, 1000)
        val his = movementOf(bob, 500)

        assertEquals(listOf(hers.id.value), history().items.map { it.id })
        assertEquals(listOf(hers.id.value, his.id.value), everything().items.map { it.id })
    }

    @Test
    fun `runs in one transaction`() {
        history()

        assertEquals(1, transactions.transactions)
    }
}
