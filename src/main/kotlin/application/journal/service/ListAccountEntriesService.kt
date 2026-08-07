package altak.ledger.application.journal.service

import altak.ledger.application.account.AccountNotFound
import altak.ledger.application.account.byIdOrReference
import altak.ledger.application.journal.EntryQueryDto
import altak.ledger.application.journal.ViewEntryDto
import altak.ledger.domain.Page
import altak.ledger.application.journal.toViewDto
import altak.ledger.application.shared.CursorDto
import altak.ledger.domain.TransactionManager
import altak.ledger.domain.account.AccountRepository
import altak.ledger.domain.journal.EntryId
import altak.ledger.domain.journal.JournalEntryRepository

data class ListAccountEntries(val query: EntryQueryDto, val cursor: CursorDto)

private val ListAccountEntries.page get() = cursor.toDomain(::EntryId)

class ListAccountEntriesService(
    private val accounts: AccountRepository,
    private val entries: JournalEntryRepository,
    private val transaction: TransactionManager,
) {

    fun execute(command: ListAccountEntries): Page<ViewEntryDto> = transaction {
        with(command) {
            val holder = accounts.byIdOrReference(query.account) ?: throw AccountNotFound(query.account)

            entries.byAccount(holder.id, page).map { it.toViewDto() }
        }
    }
}
