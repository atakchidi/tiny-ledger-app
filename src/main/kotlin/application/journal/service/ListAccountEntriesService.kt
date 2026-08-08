package altak.ledger.application.journal.service

import altak.ledger.application.account.AccountNotFound
import altak.ledger.application.account.byIdOrReference
import altak.ledger.application.journal.EntryQueryDto
import altak.ledger.application.journal.References
import altak.ledger.application.journal.toViewDto
import altak.ledger.application.shared.CursorDto
import altak.ledger.domain.TransactionManager
import altak.ledger.domain.account.AccountRepository
import altak.ledger.domain.journal.EntryId
import altak.ledger.domain.journal.JournalEntry
import altak.ledger.domain.journal.JournalEntryRepository

data class ListAccountEntries(val query: EntryQueryDto, val cursor: CursorDto)

private val ListAccountEntries.page get() = cursor.toDomain(::EntryId)

// One lookup per account the page touches, rather than one per line; a database would read them in
// a single query.
private fun AccountRepository.referencesOn(entries: List<JournalEntry>): References {
    val references = entries.flatMap { it.lines }
        .map { it.accountId }
        .distinct()
        .associateWith { byId(it)?.reference ?: error("Entry line names account $it, which is not on the books") }

    return references::getValue
}

class ListAccountEntriesService(
    private val accounts: AccountRepository,
    private val entries: JournalEntryRepository,
    private val transaction: TransactionManager,
) {

    fun execute(command: ListAccountEntries) = transaction {
        with(command) {
            val holder = accounts.byIdOrReference(query.account) ?: throw AccountNotFound(query.account)

            val history = entries.byAccount(holder.id, page)
            val references = accounts.referencesOn(history.items)

            history.map { it.toViewDto(references) }
        }
    }
}
