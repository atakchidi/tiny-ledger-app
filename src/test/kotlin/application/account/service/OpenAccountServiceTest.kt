package altak.ledger.application.account.service

import altak.ledger.CountingTransactionManager
import altak.ledger.NOW
import altak.ledger.accountFactory
import altak.ledger.application.account.OpenAccountDto
import altak.ledger.domain.Money
import altak.ledger.domain.account.AccountId
import altak.ledger.domain.account.AccountReference
import altak.ledger.domain.account.AccountType
import altak.ledger.infrastructure.persistence.InMemoryAccountRepository
import java.util.Currency
import kotlin.test.Test
import kotlin.test.assertEquals

class OpenAccountServiceTest {

    private val eur = Currency.getInstance("EUR")
    private val accounts = InMemoryAccountRepository()
    private val transactions = CountingTransactionManager()
    private val service = OpenAccountService(accounts, accountFactory(), transactions)

    private fun open(name: String = "Alice", currency: String = "EUR", reference: String = "ACC-000123") =
        service.execute(OpenAccount(OpenAccountDto(name, Currency.getInstance(currency), reference)))

    @Test
    fun `opens an account for the holder, owing them nothing yet`() {
        val alice = open()

        assertEquals("Alice", alice.name)
        assertEquals("ACC-000123", alice.reference)
        assertEquals(eur, alice.currency)
        assertEquals(AccountType.LIABILITY, alice.type)
        assertEquals("0.00", alice.balance)
        assertEquals(NOW, alice.createdAt)
    }

    @Test
    fun `keeps the account for later, under both keys it is addressed by`() {
        val alice = open()

        assertEquals("Alice", accounts.byId(AccountId(alice.id))?.name)
        assertEquals(Money.zero(eur), accounts.byId(AccountId(alice.id))?.balance)
        assertEquals(alice.id, accounts.byReference(AccountReference("ACC-000123"))?.id?.value)
    }

    @Test
    fun `runs in one transaction`() {
        open()

        assertEquals(1, transactions.transactions)
    }
}
