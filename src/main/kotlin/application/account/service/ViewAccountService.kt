package altak.ledger.application.account.service

import altak.ledger.application.account.find
import altak.ledger.application.account.toViewDto
import altak.ledger.domain.TransactionManager
import altak.ledger.domain.account.AccountRepository

data class ViewAccount(val id: String)

class ViewAccountService(
    private val accounts: AccountRepository,
    private val transaction: TransactionManager,
) {

    fun execute(query: ViewAccount) = transaction {
        with(query) { accounts.find(id).toViewDto() }
    }
}
