package altak.ledger.application.entry.service

import altak.ledger.application.account.AccountNotFound
import altak.ledger.application.account.byIdOrReference
import altak.ledger.application.entry.ViewEntryDto
import altak.ledger.domain.Page
import altak.ledger.application.entry.toViewDto
import altak.ledger.application.shared.CursorDto
import altak.ledger.domain.TransactionManager
import altak.ledger.domain.account.AccountRepository
import altak.ledger.domain.entry.EntryId
import altak.ledger.domain.entry.JournalEntryRepository

data class ListAccountEntries(val accountId: String, val cursor: CursorDto = CursorDto())

private val ListAccountEntries.page get() = cursor.toDomain(::EntryId)

class ListAccountEntriesService(
    private val accounts: AccountRepository,
    private val entries: JournalEntryRepository,
    private val transaction: TransactionManager,
) {

    fun execute(command: ListAccountEntries): Page<ViewEntryDto> = transaction {
        with(command) {
            val holder = accounts.byIdOrReference(accountId) ?: throw AccountNotFound(accountId)

            entries.byAccount(holder.id, page).map { it.toViewDto() }
        }
    }
}
