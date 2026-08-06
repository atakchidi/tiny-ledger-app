package altak.ledger.application.account.service

import altak.ledger.application.account.AccountNotFound
import altak.ledger.application.account.ViewBalanceDto
import altak.ledger.application.account.toAccountId
import altak.ledger.application.account.toBalanceViewDto
import altak.ledger.domain.TransactionManager
import altak.ledger.domain.account.AccountRepository

data class ViewBalance(val accountId: String)

private val ViewBalance.id get() = accountId.toAccountId()

class ViewBalanceService(
    private val accounts: AccountRepository,
    private val transaction: TransactionManager,
) {

    fun execute(command: ViewBalance): ViewBalanceDto = transaction {
        with(command) { accounts.byId(id)?.toBalanceViewDto() ?: throw AccountNotFound(accountId) }
    }
}
