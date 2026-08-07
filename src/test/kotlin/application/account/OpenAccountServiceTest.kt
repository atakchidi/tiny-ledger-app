package altak.ledger.application.account

import altak.ledger.CountingTransactionManager
import altak.ledger.NOW
import altak.ledger.fixedClock
import altak.ledger.application.account.service.OpenAccountService
import java.util.Currency
import altak.ledger.domain.account.AccountReference
import altak.ledger.domain.account.AccountType
import altak.ledger.infrastructure.persistence.InMemoryAccountRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OpenAccountServiceTest {

    private val eur = Currency.getInstance("EUR")
    private val accounts = InMemoryAccountRepository()
    private val transactions = CountingTransactionManager()
    private val service = OpenAccountService(accounts, transactions, fixedClock())

    @Test
    fun `opens an account owing the holder nothing yet`() {
        val alice = service.execute(OpenAccountDto("Alice", Currency.getInstance("EUR")))

        assertEquals("Alice", alice.name)
        assertEquals(eur, alice.currency)
        assertEquals("LIABILITY", alice.type)
        assertEquals(NOW.toString(), alice.createdAt)
    }

    @Test
    fun `keeps the account for later`() {
        val alice = service.execute(OpenAccountDto("Alice", Currency.getInstance("EUR")))

        val kept = accounts.byId(alice.id.toAccountId())

        assertEquals("Alice", kept?.name)
        assertEquals(AccountType.LIABILITY, kept?.type)
    }

    @Test
    fun `takes the reference the caller brought, in canonical form`() {
        val alice = service.execute(OpenAccountDto("Alice", Currency.getInstance("EUR"), "acc-000123"))

        assertEquals("ACC-000123", alice.reference)
        assertEquals(alice.id, accounts.byReference(AccountReference("ACC-000123"))?.id?.toString())
    }

    @Test
    fun `gives an account a reference of its own when the caller brings none`() {
        val alice = service.execute(OpenAccountDto("Alice", Currency.getInstance("EUR")))

        assertTrue(alice.reference.startsWith("ACC-"))
    }

    @Test
    fun `refuses to open a second account under the same reference`() {
        service.execute(OpenAccountDto("Alice", Currency.getInstance("EUR"), "acc-000123"))

        assertFailsWith<AccountAlreadyOpen> { service.execute(OpenAccountDto("Bob", Currency.getInstance("EUR"), "ACC-000123")) }
    }

    @Test
    fun `runs in one transaction`() {
        service.execute(OpenAccountDto("Alice", Currency.getInstance("EUR")))

        assertEquals(1, transactions.transactions)
    }
}
