package altak.ledger.application.account.service

import altak.ledger.CountingTransactionManager
import altak.ledger.NOW
import altak.ledger.accountFactory
import altak.ledger.domain.Money
import altak.ledger.domain.account.AccountReference
import altak.ledger.domain.account.AccountType
import altak.ledger.domain.account.ChartOfAccounts
import altak.ledger.domain.journal.MovementType
import altak.ledger.fixedClock
import altak.ledger.infrastructure.persistence.InMemoryAccountRepository
import java.util.Currency
import kotlin.test.Test
import kotlin.test.assertEquals

class ViewAccountServiceTest {

    private val eur = Currency.getInstance("EUR")
    private val clock = fixedClock()
    private val factory = accountFactory(clock)
    private val accounts = InMemoryAccountRepository()
    private val transactions = CountingTransactionManager()
    private val service = ViewAccountService(accounts, transactions)

    private val chart = ChartOfAccounts { role, currency -> factory.internal(role, currency) }

    private val alice = factory.forHolder("Alice", eur, AccountReference("ACC-ALICE")).also(accounts::save)

    @Test
    fun `shows the account as it stands`() {
        alice.move(MovementType.DEPOSIT, Money(1050, eur), chart, clock)

        val view = service.execute(ViewAccount(alice.id.toString()))

        assertEquals(alice.id.value, view.id)
        assertEquals("ACC-ALICE", view.reference)
        assertEquals("Alice", view.name)
        assertEquals(eur, view.currency)
        assertEquals(AccountType.LIABILITY, view.type)
        assertEquals("10.50", view.balance)
        assertEquals(NOW, view.createdAt)
    }

    @Test
    fun `runs in one transaction`() {
        service.execute(ViewAccount(alice.id.toString()))

        assertEquals(1, transactions.transactions)
    }
}
