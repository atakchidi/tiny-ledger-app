package altak.ledger.application.account

import altak.ledger.CountingTransactionManager
import altak.ledger.NOW
import altak.ledger.fixedClock
import altak.ledger.application.account.service.ViewAccount
import altak.ledger.application.account.service.ViewAccountService
import altak.ledger.domain.Money
import altak.ledger.domain.account.Account
import altak.ledger.domain.currencyOf
import altak.ledger.infrastructure.persistence.InMemoryAccountRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ViewAccountServiceTest {

    private val eur = currencyOf("EUR")
    private val clock = fixedClock()
    private val accounts = InMemoryAccountRepository()
    private val transactions = CountingTransactionManager()
    private val service = ViewAccountService(accounts, transactions)

    private val alice = Account.forHolder("Alice", eur, clock).also(accounts::save)

    @Test
    fun `shows the account as it stands`() {
        accounts.save(alice.copy(balance = Money(1050, eur)))

        val view = service.execute(ViewAccount(alice.id.toString()))

        assertEquals(alice.id.toString(), view.id)
        assertEquals("Alice", view.name)
        assertEquals("EUR", view.currency)
        assertEquals("LIABILITY", view.type)
        assertEquals("10.50", view.balance)
        assertEquals(NOW.toString(), view.createdAt)
    }

    @Test
    fun `refuses an id that names no account`() {
        assertFailsWith<AccountNotFound> { service.execute(ViewAccount("not-an-account")) }
        assertFailsWith<AccountNotFound> {
            service.execute(ViewAccount(Account.forHolder("Ghost", eur, clock).id.toString()))
        }
    }

    @Test
    fun `runs in one transaction`() {
        service.execute(ViewAccount(alice.id.toString()))

        assertEquals(1, transactions.transactions)
    }
}
