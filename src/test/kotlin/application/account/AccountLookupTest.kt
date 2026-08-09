package altak.ledger.application.account

import altak.ledger.accountFactory
import altak.ledger.domain.account.AccountReference
import altak.ledger.fixedClock
import altak.ledger.infrastructure.persistence.InMemoryAccountRepository
import java.util.Currency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AccountLookupTest {

    private val eur = Currency.getInstance("EUR")
    private val factory = accountFactory(fixedClock())
    private val accounts = InMemoryAccountRepository()

    private val alice = factory.forHolder("Alice", eur, AccountReference("ACC-ALICE")).also(accounts::save)

    @Test
    fun `finds an account by its id`() {
        assertEquals(alice.id, accounts.find(alice.id.toString()).id)
    }

    @Test
    fun `finds an account by the reference it is known by outside`() {
        assertEquals(alice.id, accounts.find("ACC-ALICE").id)
    }

    @Test
    fun `refuses a key that names no account`() {
        val ghost = factory.forHolder("Ghost", eur, AccountReference("ACC-GHOST"))

        assertFailsWith<AccountNotFound> { accounts.find(ghost.id.toString()) }
        assertFailsWith<AccountNotFound> { accounts.find("ACC-NOBODY") }
    }

    @Test
    fun `refuses a key that is neither an id nor a reference`() {
        assertFailsWith<AccountNotFound> { accounts.find("not-an-account") }
        assertFailsWith<AccountNotFound> { accounts.find("") }
    }
}
