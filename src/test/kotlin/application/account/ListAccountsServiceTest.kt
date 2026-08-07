package altak.ledger.application.account

import altak.ledger.CountingTransactionManager
import altak.ledger.NOW
import altak.ledger.fixedClock
import altak.ledger.application.account.service.ListAccounts
import altak.ledger.application.account.service.ListAccountsService
import altak.ledger.domain.Money
import altak.ledger.domain.account.Account
import java.util.Currency
import java.math.BigDecimal
import altak.ledger.infrastructure.persistence.InMemoryAccountRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class ListAccountsServiceTest {

    private val eur = Currency.getInstance("EUR")
    private val clock = fixedClock()
    private val accounts = InMemoryAccountRepository()
    private val transactions = CountingTransactionManager()
    private val service = ListAccountsService(accounts, transactions)

    @Test
    fun `has nothing to list on an empty ledger`() {
        assertEquals(emptyList(), service.execute(ListAccounts()).accounts)
    }

    @Test
    fun `lists holder accounts and the cash behind them alike`() {
        Account.forHolder("Alice", eur, clock).also(accounts::save)
        Account.forCash(eur, clock).copy(balance = Money(1050, eur)).also(accounts::save)

        val listed = service.execute(ListAccounts()).accounts

        assertEquals(setOf("Alice", "Cash EUR"), listed.map { it.name }.toSet())
        assertEquals(BigDecimal("10.50"), listed.single { it.type == "ASSET" }.balance)
    }

    @Test
    fun `runs in one transaction`() {
        service.execute(ListAccounts())

        assertEquals(1, transactions.transactions)
    }
}
