package altak.ledger.application.ledger.service

import altak.ledger.application.account.AccountNotFound
import altak.ledger.application.account.toAccountId
import altak.ledger.application.ledger.ViewHistoryDto
import altak.ledger.application.ledger.toCursor
import altak.ledger.application.ledger.toHistoryViewDto
import altak.ledger.domain.TransactionManager
import altak.ledger.domain.account.AccountRepository
import altak.ledger.domain.ledger.JournalEntryRepository
import altak.ledger.domain.ledger.Page

data class ViewHistory(val accountId: String, val after: String? = null, val limit: Int = Page.DEFAULT_LIMIT)

private val ViewHistory.id get() = accountId.toAccountId()
private val ViewHistory.page get() = Page(after = after?.toCursor(), limit = limit)

class ViewHistoryService(
    private val accounts: AccountRepository,
    private val entries: JournalEntryRepository,
    private val transaction: TransactionManager,
) {

    fun execute(command: ViewHistory): ViewHistoryDto = transaction {
        with(command) {
            val holder = accounts.byId(id) ?: throw AccountNotFound(accountId)

            entries.byAccount(holder.id, page).toHistoryViewDto(page)
        }
    }
}
