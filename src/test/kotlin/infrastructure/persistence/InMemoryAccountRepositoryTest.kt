package altak.ledger.infrastructure.persistence

import altak.ledger.accountFactory
import altak.ledger.domain.Cursor
import altak.ledger.domain.account.Account
import altak.ledger.domain.account.AccountId
import altak.ledger.domain.account.AccountReference
import altak.ledger.domain.account.AccountRole
import altak.ledger.fixedClock
import java.util.Currency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class InMemoryAccountRepositoryTest {

    private val eur = Currency.getInstance("EUR")
    private val usd = Currency.getInstance("USD")
    private val clock = fixedClock()
    private val factory = accountFactory(clock)
    private val repository = InMemoryAccountRepository()

    private val alice = factory.forHolder("Alice", eur, AccountReference("ACC-ALICE"))

    @Test
    fun `hands back nothing for an account it has not seen`() {
        assertNull(repository.byId(AccountId(Uuid.generateV7NonMonotonicAt(clock.now()))))
        assertNull(repository.byReference(AccountRole.CASH.referenceFor(eur)))
        assertEquals(emptyList(), repository.all(Cursor(50)).items)
    }

    @Test
    fun `finds a saved account by id`() {
        repository.save(alice)

        assertEquals(alice, repository.byId(alice.id))
    }

    @Test
    fun `reads a batch of accounts at once, asking once for a repeated id and skipping ones it has not seen`() {
        val cash = factory.internal(AccountRole.CASH, eur)
        listOf(alice, cash).forEach(repository::save)
        val unknown = AccountId(Uuid.generateV7NonMonotonicAt(clock.now()))

        assertEquals(listOf(alice, cash), repository.byIds(listOf(alice.id, cash.id, alice.id, unknown)))
        assertEquals(emptyList(), repository.byIds(emptyList()))
    }

    @Test
    fun `finds a saved account by the reference it is known by outside`() {
        val cashEur = factory.internal(AccountRole.CASH, eur)
        val cashUsd = factory.internal(AccountRole.CASH, usd)
        listOf(cashEur, cashUsd, alice).forEach(repository::save)

        assertEquals(cashEur, repository.byReference(AccountRole.CASH.referenceFor(eur)))
        assertEquals(cashUsd, repository.byReference(AccountRole.CASH.referenceFor(usd)))
        assertEquals(alice, repository.byReference(alice.reference))
    }

    @Test
    fun `saving the same account again replaces it`() {
        repository.save(alice)
        repository.save(Account(alice.id, alice.reference, "Alice Smith", alice.currency, alice.type, alice.createdAt))

        assertEquals("Alice Smith", repository.byId(alice.id)?.name)
        assertEquals(1, repository.all(Cursor(50)).items.size)
    }
}
