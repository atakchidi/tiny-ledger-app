package altak.ledger.application.account

import altak.ledger.CountingTransactionManager
import altak.ledger.NOW
import altak.ledger.fixedClock
import altak.ledger.ids
import altak.ledger.application.account.service.ViewAccount
import altak.ledger.application.account.service.ViewAccountService
import java.math.BigDecimal
import java.util.Currency
import altak.ledger.domain.Money
import altak.ledger.domain.account.Account
import altak.ledger.domain.account.AccountReference
import altak.ledger.domain.account.AccountType
import altak.ledger.infrastructure.persistence.InMemoryAccountRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ViewAccountServiceTest {

    private val eur = Currency.getInstance("EUR")
    private val clock = fixedClock()
    private val accounts = InMemoryAccountRepository()
    private val transactions = CountingTransactionManager()
    private val service = ViewAccountService(accounts, transactions)

    private val alice = Account.forHolder("Alice", eur, ids, clock, AccountReference("ACC-Alice".uppercase())).also(accounts::save)

    @Test
    fun `shows the account as it stands`() {
        accounts.save(alice.copy(balance = Money(1050, eur)))

        val view = service.execute(ViewAccount(alice.id.toString()))

        assertEquals(alice.id.value, view.id)
        assertEquals("Alice", view.name)
        assertEquals(eur, view.currency)
        assertEquals(AccountType.LIABILITY, view.type)
        assertEquals(BigDecimal("10.50"), view.balance)
        assertEquals(NOW, view.createdAt)
    }

    @Test
    fun `finds an account by the reference it is known by outside`() {
        val view = service.execute(ViewAccount(alice.reference.toString()))

        assertEquals(alice.id.value, view.id)
        assertEquals(alice.reference.toString(), view.reference)
    }

    @Test
    fun `refuses an id that names no account`() {
        assertFailsWith<AccountNotFound> { service.execute(ViewAccount("not-an-account")) }
        assertFailsWith<AccountNotFound> {
            service.execute(ViewAccount(Account.forHolder("Ghost", eur, ids, clock, AccountReference("ACC-Ghost".uppercase())).id.toString()))
        }
    }

    @Test
    fun `runs in one transaction`() {
        service.execute(ViewAccount(alice.id.toString()))

        assertEquals(1, transactions.transactions)
    }
}
