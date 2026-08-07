package altak.ledger.application.journal.service

import altak.ledger.application.account.AccountNotFound
import altak.ledger.application.account.byIdOrReference
import altak.ledger.application.journal.BalanceQueryDto
import altak.ledger.application.journal.toViewDto
import altak.ledger.application.shared.CursorDto
import altak.ledger.domain.TransactionManager
import altak.ledger.domain.account.AccountId
import altak.ledger.domain.account.AccountRepository
import altak.ledger.domain.journal.BalanceQuery
import altak.ledger.domain.journal.BalancesCalculator
import kotlin.time.Clock

data class ListBalances(val query: BalanceQueryDto, val cursor: CursorDto)

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
