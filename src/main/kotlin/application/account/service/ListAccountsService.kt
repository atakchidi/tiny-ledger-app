package altak.ledger.application.account.service

import altak.ledger.application.account.ViewAccountDto
import altak.ledger.application.account.toViewDto
import altak.ledger.domain.TransactionManager
import altak.ledger.domain.account.AccountRepository

class ListAccountsService(
    private val accounts: AccountRepository,
    private val transaction: TransactionManager,
) {
    fun execute(): List<ViewAccountDto> = transaction {
        accounts.all().map { it.toViewDto() }
    }
}
