package altak.ledger.application.account

import altak.ledger.CountingTransactionManager
import altak.ledger.NOW
import altak.ledger.fixedClock
import altak.ledger.application.account.service.ViewBalance
import altak.ledger.application.account.service.ViewBalanceService
import altak.ledger.domain.Money
import java.util.Currency
import java.math.BigDecimal
import altak.ledger.domain.account.Account
import altak.ledger.infrastructure.persistence.InMemoryAccountRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ViewBalanceServiceTest {

    private val eur = Currency.getInstance("EUR")
    private val jpy = Currency.getInstance("JPY")
    private val clock = fixedClock()
    private val accounts = InMemoryAccountRepository()
    private val transactions = CountingTransactionManager()
    private val service = ViewBalanceService(accounts, transactions)

    private val alice = Account.forHolder("Alice", eur, clock).also(accounts::save)

    @Test
    fun `shows what the ledger owes the holder`() {
        accounts.save(alice.copy(balance = Money(1050, eur)))

        val balance = service.execute(ViewBalance(alice.id.toString()))

        assertEquals(alice.id.toString(), balance.accountId)
        assertEquals(eur, balance.currency)
        assertEquals(BigDecimal("10.50"), balance.amount)
    }

    @Test
    fun `shows nothing owed on a fresh account`() {
        assertEquals(BigDecimal("0.00"), service.execute(ViewBalance(alice.id.toString())).amount)
    }

    @Test
    fun `shows a balance the holder has overdrawn`() {
        accounts.save(alice.copy(balance = Money(-250, eur)))

        assertEquals(BigDecimal("-2.50"), service.execute(ViewBalance(alice.id.toString())).amount)
    }

    @Test
    fun `shows the amount in the currency's own precision`() {
        val yuki = Account.forHolder("Yuki", jpy, clock).copy(balance = Money(1000, jpy)).also(accounts::save)

        assertEquals(BigDecimal("1000"), service.execute(ViewBalance(yuki.id.toString())).amount)
    }

    @Test
    fun `refuses an id that names no account`() {
        assertFailsWith<AccountNotFound> { service.execute(ViewBalance("not-an-account")) }
    }
}
