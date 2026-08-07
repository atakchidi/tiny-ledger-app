package altak.ledger.application.account

import altak.ledger.CountingTransactionManager
import altak.ledger.application.account.service.ListAccounts
import altak.ledger.application.account.service.ListAccountsService
import altak.ledger.application.shared.CursorDto
import altak.ledger.domain.Money
import altak.ledger.domain.account.Account
import altak.ledger.domain.account.AccountReference
import altak.ledger.domain.account.AccountRole
import altak.ledger.domain.account.AccountType
import altak.ledger.fixedClock
import altak.ledger.ids
import altak.ledger.infrastructure.persistence.InMemoryAccountRepository
import java.math.BigDecimal
import java.util.Currency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ListAccountsServiceTest {

    private val eur = Currency.getInstance("EUR")
    private val clock = fixedClock()
    private val accounts = InMemoryAccountRepository()
    private val transactions = CountingTransactionManager()
    private val service = ListAccountsService(accounts, transactions)

    private fun list(cursor: CursorDto = CursorDto()) = service.execute(ListAccounts(cursor))

    @Test
    fun `has nothing to list on an empty ledger`() {
        assertEquals(emptyList(), list().items)
        assertNull(list().nextCursor)
    }

    @Test
    fun `lists holder accounts and the cash behind them alike`() {
        Account.forHolder("Alice", eur, ids, clock, AccountReference("ACC-Alice".uppercase())).also(accounts::save)
        Account.internal(AccountRole.CASH, eur, ids, clock)
            .copy(balance = Money(1050, eur))
            .also(accounts::save)

        val listed = list().items

        assertEquals(setOf("Alice", "Cash EUR"), listed.map { it.name }.toSet())
        assertEquals(BigDecimal("10.50"), listed.single { it.type == AccountType.ASSET }.balance)
    }

    @Test
    fun `hands the accounts back a page at a time`() {
        repeat(3) { Account.forHolder("Holder $it", eur, ids, clock, AccountReference("ACC-HOLDER-$it")).also(accounts::save) }

        val page = list(CursorDto(limit = 2))

        assertEquals(2, page.items.size)
        assertEquals(page.items.last().id.toString(), page.nextCursor)
    }

    @Test
    fun `runs in one transaction`() {
        list()

        assertEquals(1, transactions.transactions)
    }
}
