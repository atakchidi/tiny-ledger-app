package altak.ledger.application.ledger.service

import altak.ledger.application.account.AccountNotFound
import altak.ledger.application.account.byIdOrReference
import altak.ledger.application.ledger.ViewHistoryDto
import altak.ledger.application.ledger.toHistoryViewDto
import altak.ledger.application.shared.CursorDto
import altak.ledger.domain.TransactionManager
import altak.ledger.domain.account.AccountRepository
import altak.ledger.domain.ledger.EntryId
import altak.ledger.domain.ledger.JournalEntryRepository

data class ViewHistory(val accountId: String, val cursor: CursorDto = CursorDto())

private val ViewHistory.page get() = cursor.toDomain(::EntryId)

class ViewHistoryService(
    private val accounts: AccountRepository,
    private val entries: JournalEntryRepository,
    private val transaction: TransactionManager,
) {

    fun execute(command: ViewHistory): ViewHistoryDto = transaction {
        with(command) {
            val holder = accounts.byIdOrReference(accountId) ?: throw AccountNotFound(accountId)

            entries.byAccount(holder.id, page).toHistoryViewDto(page)
        }
    }
}
