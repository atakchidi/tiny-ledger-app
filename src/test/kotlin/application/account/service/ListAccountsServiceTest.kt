package altak.ledger.application.account.service

import altak.ledger.CountingTransactionManager
import altak.ledger.accountFactory
import altak.ledger.application.shared.CursorDto
import altak.ledger.domain.Money
import altak.ledger.domain.account.AccountReference
import altak.ledger.domain.account.AccountRole
import altak.ledger.domain.account.AccountType
import altak.ledger.domain.account.ChartOfAccounts
import altak.ledger.domain.journal.MovementType
import altak.ledger.fixedClock
import altak.ledger.infrastructure.persistence.InMemoryAccountRepository
import java.util.Currency
import kotlin.test.Test
import kotlin.test.assertEquals

class ListAccountsServiceTest {

    private val eur = Currency.getInstance("EUR")
    private val clock = fixedClock()
    private val factory = accountFactory(clock)
    private val accounts = InMemoryAccountRepository()
    private val transactions = CountingTransactionManager()
    private val service = ListAccountsService(accounts, transactions)

    private fun list() = service.execute(ListAccounts(CursorDto()))

    @Test
    fun `has nothing to list on an empty ledger`() {
        assertEquals(emptyList(), list().items)
    }

    @Test
    fun `lists holder accounts and the cash behind them alike, each with what it stands at`() {
        val alice = factory.forHolder("Alice", eur, AccountReference("ACC-ALICE")).also(accounts::save)
        val cash = factory.internal(AccountRole.CASH, eur).also(accounts::save)
        alice.move(MovementType.DEPOSIT, Money(1050, eur), ChartOfAccounts { _, _ -> cash }, clock)

        val listed = list().items

        assertEquals(setOf("Alice", "Cash EUR"), listed.map { it.name }.toSet())
        assertEquals("10.50", listed.single { it.type == AccountType.ASSET }.balance)
        assertEquals("10.50", listed.single { it.type == AccountType.LIABILITY }.balance)
    }

    @Test
    fun `runs in one transaction`() {
        list()

        assertEquals(1, transactions.transactions)
    }
}
