package altak.ledger.application.account

import altak.ledger.CountingTransactionManager
import altak.ledger.NOW
import altak.ledger.fixedClock
import altak.ledger.application.account.service.OpenAccountService
import altak.ledger.domain.LedgerException
import altak.ledger.domain.Money
import altak.ledger.domain.account.AccountType
import altak.ledger.domain.currencyOf
import altak.ledger.infrastructure.persistence.InMemoryAccountRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OpenAccountServiceTest {

    private val eur = currencyOf("EUR")
    private val accounts = InMemoryAccountRepository()
    private val transactions = CountingTransactionManager()
    private val service = OpenAccountService(accounts, transactions, fixedClock())

    @Test
    fun `opens an account owing the holder nothing yet`() {
        val alice = service.execute(OpenAccountDto("Alice", "EUR"))

        assertEquals("Alice", alice.name)
        assertEquals("EUR", alice.currency)
        assertEquals("LIABILITY", alice.type)
        assertEquals("0.00", alice.balance)
        assertEquals(NOW.toString(), alice.createdAt)
    }

    @Test
    fun `keeps the account for later`() {
        val alice = service.execute(OpenAccountDto("Alice", "EUR"))

        val kept = accounts.byId(alice.id.toAccountId())

        assertEquals("Alice", kept?.name)
        assertEquals(AccountType.LIABILITY, kept?.type)
        assertEquals(Money.zero(eur), kept?.balance)
    }

    @Test
    fun `refuses an unknown currency`() {
        assertFailsWith<LedgerException.UnknownCurrency> { service.execute(OpenAccountDto("Alice", "XYZ")) }
    }

    @Test
    fun `runs in one transaction`() {
        service.execute(OpenAccountDto("Alice", "EUR"))

        assertEquals(1, transactions.transactions)
    }
}
