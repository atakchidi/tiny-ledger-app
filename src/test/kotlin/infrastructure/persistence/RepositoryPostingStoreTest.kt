package altak.ledger.infrastructure.persistence

import altak.ledger.accountFactory
import altak.ledger.domain.Cursor
import altak.ledger.domain.Money
import altak.ledger.domain.account.AccountReference
import altak.ledger.domain.account.AccountRole
import altak.ledger.domain.account.ChartOfAccounts
import altak.ledger.domain.journal.MovementType
import altak.ledger.domain.journal.PostingFactory
import altak.ledger.fixedClock
import altak.ledger.journalEntryFactory
import java.util.Currency
import kotlin.test.Test
import kotlin.test.assertEquals

class RepositoryPostingStoreTest {

    private val eur = Currency.getInstance("EUR")
    private val clock = fixedClock()
    private val factory = accountFactory(clock)
    private val accounts = InMemoryAccountRepository()
    private val entries = InMemoryJournalEntryRepository()
    private val store = RepositoryPostingStore(accounts, entries)

    private val chart = ChartOfAccounts { role, currency -> factory.internal(role, currency) }
    private val postings = PostingFactory(chart, journalEntryFactory(clock), clock)

    private val alice = factory.forHolder("Alice", eur, AccountReference("ACC-ALICE")).also(accounts::save)

    @Test
    fun `keeps the entry where both accounts can find it`() {
        val posting = postings.create(alice, MovementType.DEPOSIT, Money(1000, eur))

        store.store(posting)

        val cash = accounts.byReference(AccountRole.CASH.referenceFor(eur))!!
        assertEquals(listOf(posting.entry), entries.byAccount(alice.id, Cursor(50)).items)
        assertEquals(listOf(posting.entry), entries.byAccount(cash.id, Cursor(50)).items)
    }

    @Test
    fun `keeps every account the posting moved, the counterpart included`() {
        val posting = postings.create(alice, MovementType.DEPOSIT, Money(1000, eur))

        store.store(posting)

        assertEquals(
            setOf("ACC-ALICE", "CASH-EUR"),
            accounts.all(Cursor(50)).items.map { it.reference.toString() }.toSet(),
        )
        assertEquals(Money(1000, eur), accounts.byReference(AccountRole.CASH.referenceFor(eur))?.balance)
    }
}
