package altak.ledger.application.journal.service

import altak.ledger.application.account.find
import altak.ledger.application.journal.SearchAccountEntriesDto
import altak.ledger.application.journal.Accounts
import altak.ledger.application.journal.toViewDto
import altak.ledger.application.shared.CursorDto
import altak.ledger.domain.TransactionManager
import altak.ledger.domain.account.AccountRepository
import altak.ledger.domain.journal.EntryId
import altak.ledger.domain.journal.JournalEntry
import altak.ledger.domain.journal.JournalEntryRepository

data class ListAccountEntries(val search: SearchAccountEntriesDto, val cursor: CursorDto)

private val ListAccountEntries.page get() = cursor.toDomain(::EntryId)

private fun AccountRepository.accountsOn(entries: List<JournalEntry>): Accounts {
    val accounts = byIds(entries.flatMap { it.lines }.map { it.accountId }.toSet()).associateBy { it.id }

    return { id -> accounts[id] ?: error("Entry line names account $id, which is not on the books") }
}

class ListAccountEntriesService(
    private val accounts: AccountRepository,
    private val entries: JournalEntryRepository,
    private val transaction: TransactionManager,
) {

    fun execute(query: ListAccountEntries) = transaction {
        with(query) {
            val holder = search.account?.let { accounts.find(it) }

            val history = holder?.let { entries.byAccount(it.id, page) } ?: entries.all(page)
            val onThePage = accounts.accountsOn(history.items)

            history.map { it.toViewDto(onThePage) }
        }
    }
}
