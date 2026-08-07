package altak.ledger.application.account.service

import altak.ledger.application.account.AccountNotFound
import altak.ledger.application.account.ViewBalanceDto
import altak.ledger.application.account.byIdOrReference
import altak.ledger.application.account.toBalanceViewDto
import altak.ledger.domain.TransactionManager
import altak.ledger.domain.account.AccountRepository

data class ViewBalance(val accountId: String)

class ViewBalanceService(
    private val accounts: AccountRepository,
    private val transaction: TransactionManager,
) {

    fun execute(command: ViewBalance): ViewBalanceDto = transaction {
        with(command) { accounts.byIdOrReference(accountId)?.toBalanceViewDto() ?: throw AccountNotFound(accountId) }
    }
}
