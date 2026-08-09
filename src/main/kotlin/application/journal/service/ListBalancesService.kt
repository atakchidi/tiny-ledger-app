package altak.ledger.application.journal.service

import altak.ledger.application.account.find
import altak.ledger.application.journal.SearchBalancesDto
import altak.ledger.application.journal.toViewDto
import altak.ledger.application.shared.CursorDto
import altak.ledger.domain.LedgerCalendar
import altak.ledger.domain.TransactionManager
import altak.ledger.domain.account.AccountId
import altak.ledger.domain.account.AccountRepository
import altak.ledger.domain.journal.BalanceQuery
import altak.ledger.domain.journal.BalancesCalculator

data class ListBalances(val search: SearchBalancesDto, val cursor: CursorDto)

private val ListBalances.page get() = cursor.toDomain(::AccountId)

class ListBalancesService(
    private val accounts: AccountRepository,
    private val balances: BalancesCalculator,
    private val transaction: TransactionManager,
    private val calendar: LedgerCalendar,
) {
    fun execute(query: ListBalances) = transaction {
        with(query) {
            val accountId = search.account?.let { accounts.find(it).id }
            val criteria = BalanceQuery(onDate = search.onDate ?: calendar.today(), accountId = accountId)

            balances.calculate(criteria, page).map { it.toViewDto() }
        }
    }
}
