package altak.ledger.application.account.service

import altak.ledger.application.account.AccountNotFound
import altak.ledger.application.account.ViewAccountDto
import altak.ledger.application.account.toAccountId
import altak.ledger.application.account.toViewDto
import altak.ledger.domain.TransactionManager
import altak.ledger.domain.account.AccountRepository

data class ViewAccount(val id: String)

private val ViewAccount.accountId get() = id.toAccountId()

class ViewAccountService(
    private val accounts: AccountRepository,
    private val transaction: TransactionManager,
) {

    fun execute(command: ViewAccount): ViewAccountDto = transaction {
        with(command) { accounts.byId(accountId)?.toViewDto() ?: throw AccountNotFound(id) }
    }
}
