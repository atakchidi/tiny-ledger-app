package altak.ledger.infrastructure.persistence

import altak.ledger.accountFactory
import altak.ledger.domain.account.AccountRole
import altak.ledger.fixedClock
import java.util.Currency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class RepositoryChartOfAccountsTest {

    private val eur = Currency.getInstance("EUR")
    private val usd = Currency.getInstance("USD")
    private val accounts = InMemoryAccountRepository()
    private val chart = RepositoryChartOfAccounts(accounts, accountFactory(fixedClock()))

    @Test
    fun `opens the account a role stands for when the books do not hold it yet`() {
        val cash = chart.of(AccountRole.CASH, eur)

        assertEquals("CASH-EUR", cash.reference.toString())
    }

    @Test
    fun `hands back the account already on the books rather than opening a second`() {
        val opened = chart.of(AccountRole.CASH, eur).also(accounts::save)

        assertEquals(opened.id, chart.of(AccountRole.CASH, eur).id)
    }

    @Test
    fun `keeps one account per currency for the same role`() {
        val euros = chart.of(AccountRole.CASH, eur).also(accounts::save)
        val dollars = chart.of(AccountRole.CASH, usd).also(accounts::save)

        assertNotEquals(euros.id, dollars.id)
        assertEquals("CASH-USD", dollars.reference.toString())
    }
}
