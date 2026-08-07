package altak.ledger.application.balance.service

import altak.ledger.application.account.AccountNotFound
import altak.ledger.application.balance.BalanceQueryDto
import altak.ledger.application.balance.ViewBalanceDto
import altak.ledger.application.account.byIdOrReference
import altak.ledger.application.balance.toViewDto
import altak.ledger.application.shared.CursorDto
import altak.ledger.domain.Page
import altak.ledger.domain.TransactionManager
import altak.ledger.domain.account.AccountId
import altak.ledger.domain.account.AccountRepository
import altak.ledger.domain.entry.BalanceQuery
import altak.ledger.domain.entry.BalancesCalculator
import kotlin.time.Clock

data class ListBalances(val query: BalanceQueryDto = BalanceQueryDto(), val cursor: CursorDto = CursorDto())

private val ListBalances.page get() = cursor.toDomain(::AccountId)

class ListBalancesService(
    private val accounts: AccountRepository,
    private val balances: BalancesCalculator,
    private val transactions: TransactionManager,
    private val clock: Clock,
) {
    fun execute(command: ListBalances) = transactions {
        with(command) {
            val accountId = query.account?.let { accounts.byIdOrReference(query.account)?.id ?: throw AccountNotFound(it) }
            val criteria = BalanceQuery(onDate = query.onDate ?: clock.now(), accountId = accountId)

            balances.calculate(criteria, page).map { it.toViewDto() }
        }
    }
}
