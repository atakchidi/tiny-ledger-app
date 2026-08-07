package altak.ledger.infrastructure.persistence

import altak.ledger.CountingTransactionManager
import altak.ledger.NOW
import altak.ledger.fixedClock
import altak.ledger.domain.Money
import altak.ledger.domain.account.Account
import altak.ledger.domain.account.AccountRole
import altak.ledger.domain.account.AccountId
import altak.ledger.domain.account.AccountType
import altak.ledger.domain.entry.Direction
import altak.ledger.domain.entry.EntryLine
import altak.ledger.domain.entry.JournalEntry
import altak.ledger.domain.Cursor
import java.util.Currency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class InMemoryAccountRepositoryTest {

    private val eur = Currency.getInstance("EUR")
    private val usd = Currency.getInstance("USD")
    private val clock = fixedClock()
    private val repository = InMemoryAccountRepository()

    private val alice = Account.forHolder("Alice", eur, clock)

    @Test
    fun `hands back nothing for an account it has not seen`() {
        assertNull(repository.byId(AccountId(Uuid.generateV7NonMonotonicAt(clock.now()))))
        assertNull(repository.byReference(AccountRole.CASH.referenceFor(eur)))
        assertEquals(emptyList(), repository.all(Cursor()).items)
    }

    @Test
    fun `finds a saved account by id`() {
        repository.save(alice)

        assertEquals(alice, repository.byId(alice.id))
    }

    @Test
    fun `finds a saved account by the reference it is known by outside`() {
        val cashEur = Account.internal(AccountRole.CASH, eur, clock)
        val cashUsd = Account.internal(AccountRole.CASH, usd, clock)
        listOf(cashEur, cashUsd, alice).forEach(repository::save)

        assertEquals(cashEur, repository.byReference(AccountRole.CASH.referenceFor(eur)))
        assertEquals(cashUsd, repository.byReference(AccountRole.CASH.referenceFor(usd)))
        assertEquals(alice, repository.byReference(alice.reference))
    }

    @Test
    fun `saving the same account again replaces it`() {
        repository.save(alice)
        repository.save(alice.copy(name = "Alice Smith"))

        assertEquals("Alice Smith", repository.byId(alice.id)?.name)
        assertEquals(1, repository.all(Cursor()).items.size)
    }
}

class InMemoryJournalEntryRepositoryTest {

    private val eur = Currency.getInstance("EUR")
    private val clock = fixedClock()
    private val repository = InMemoryJournalEntryRepository()

    private val alice = Account.forHolder("Alice", eur, clock)
    private val cash = Account.internal(AccountRole.CASH, eur, clock)
    private val bob = Account.forHolder("Bob", eur, clock)

    private fun movementOf(holder: Account, minorUnits: Long) =
        JournalEntry(
            "movement",
            listOf(
                EntryLine(holder.id, Direction.CREDIT, Money(minorUnits, eur)),
                EntryLine(cash.id, Direction.DEBIT, Money(minorUnits, eur)),
            ),
            clock,
        )

    @Test
    fun `hands back nothing for an account with no entries`() {
        assertEquals(emptyList(), repository.byAccount(alice.id, Cursor()).items)
    }

    @Test
    fun `finds an entry under every account it touches`() {
        val entry = movementOf(alice, 1000).also(repository::save)

        assertEquals(listOf(entry), repository.byAccount(alice.id, Cursor()).items)
        assertEquals(listOf(entry), repository.byAccount(cash.id, Cursor()).items)
    }

    @Test
    fun `keeps entries of other accounts out`() {
        val hers = movementOf(alice, 1000).also(repository::save)
        val his = movementOf(bob, 500).also(repository::save)

        assertEquals(listOf(hers), repository.byAccount(alice.id, Cursor()).items)
        assertEquals(listOf(his), repository.byAccount(bob.id, Cursor()).items)
        assertEquals(listOf(hers, his), repository.byAccount(cash.id, Cursor()).items)
    }

    @Test
    fun `hands back one page at a time from a cursor`() {
        val appended = (1..5).map { movementOf(alice, it * 100L).also(repository::save) }

        val firstPage = repository.byAccount(alice.id, Cursor(limit = 2)).items
        val nextPage = repository.byAccount(alice.id, Cursor(after = firstPage.last().id, limit = 2)).items

        assertEquals(appended.take(2), firstPage)
        assertEquals(appended.drop(2).take(2), nextPage)
    }

    @Test
    fun `hands back nothing once the cursor runs past the last entry`() {
        val only = movementOf(alice, 100).also(repository::save)
        val his = movementOf(bob, 100).also(repository::save)

        assertEquals(emptyList(), repository.byAccount(alice.id, Cursor(after = only.id)).items)
        assertEquals(emptyList(), repository.byAccount(alice.id, Cursor(after = his.id)).items)
    }

    @Test
    fun `keeps entries in the order they were appended`() {
        val first = movementOf(alice, 100).also(repository::save)
        val second = movementOf(alice, 200).also(repository::save)
        val third = movementOf(alice, 300).also(repository::save)

        assertEquals(listOf(first, second, third), repository.byAccount(alice.id, Cursor()).items)
    }
}
