package altak.ledger.application.account.service

import altak.ledger.application.account.toViewDto
import altak.ledger.application.shared.CursorDto
import altak.ledger.domain.TransactionManager
import altak.ledger.domain.account.AccountId
import altak.ledger.domain.account.AccountRepository

data class ListAccounts(val cursor: CursorDto)

private val ListAccounts.page get() = cursor.toDomain(::AccountId)

class ListAccountsService(
    private val accounts: AccountRepository,
    private val transaction: TransactionManager,
) {
    fun execute(query: ListAccounts) = transaction {
        with(query) { accounts.all(page).map { it.toViewDto() } }
    }
}
