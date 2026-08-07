package altak.ledger.application.account

import altak.ledger.CountingTransactionManager
import altak.ledger.NOW
import altak.ledger.application.account.service.OpenAccount
import altak.ledger.application.account.service.OpenAccountService
import altak.ledger.domain.Money
import altak.ledger.domain.account.AccountReference
import altak.ledger.domain.account.AccountType
import altak.ledger.fixedClock
import altak.ledger.ids
import altak.ledger.infrastructure.persistence.InMemoryAccountRepository
import java.math.BigDecimal
import java.util.Currency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OpenAccountServiceTest {

    private val eur = Currency.getInstance("EUR")
    private val accounts = InMemoryAccountRepository()
    private val transactions = CountingTransactionManager()
    private val service = OpenAccountService(accounts, ids, transactions, fixedClock())

    private fun open(name: String = "Alice", currency: String = "EUR", reference: String = "ACC-000123") =
        service.execute(OpenAccount(OpenAccountDto(name, Currency.getInstance(currency), reference)))

    @Test
    fun `opens an account owing the holder nothing yet`() {
        val alice = open()

        assertEquals("Alice", alice.name)
        assertEquals(eur, alice.currency)
        assertEquals(AccountType.LIABILITY, alice.type)
        assertEquals(BigDecimal("0.00"), alice.balance)
        assertEquals(NOW, alice.createdAt)
    }

    @Test
    fun `takes the reference the caller brought`() {
        val alice = open(reference = "ACC-000123")

        assertEquals("ACC-000123", alice.reference)
        assertEquals(alice.id, accounts.byReference(AccountReference("ACC-000123"))?.id?.value)
    }

    @Test
    fun `keeps the account for later`() {
        val alice = open()

        val kept = accounts.byId(alice.id.toString().toAccountId())

        assertEquals("Alice", kept?.name)
        assertEquals(AccountType.LIABILITY, kept?.type)
        assertEquals(Money.zero(eur), kept?.balance)
    }

    @Test
    fun `refuses to open a second account under the same reference`() {
        open(reference = "ACC-000123")

        assertFailsWith<AccountAlreadyOpen> { open(name = "Bob", reference = "ACC-000123") }
    }

    @Test
    fun `refuses a reference nothing could quote back`() {
        assertFailsWith<AccountReference.Malformed> { open(reference = "no") }
    }

    @Test
    fun `runs in one transaction`() {
        open()

        assertEquals(1, transactions.transactions)
    }
}
