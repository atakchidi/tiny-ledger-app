package altak.ledger.infrastructure.persistence

import altak.ledger.domain.Cursor
import altak.ledger.domain.account.AccountId
import altak.ledger.domain.journal.EntryId
import altak.ledger.domain.journal.EntryLine
import altak.ledger.domain.journal.JournalEntry
import altak.ledger.domain.journal.JournalEntryRepository
import kotlinx.datetime.LocalDate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class InMemoryJournalEntryRepository : JournalEntryRepository {

    private val entriesById = ConcurrentHashMap<EntryId, JournalEntry>()
    private val entriesByAccount = ConcurrentHashMap<AccountId, CopyOnWriteArrayList<JournalEntry>>()

    override fun save(entry: JournalEntry) {
        entriesById[entry.id] = entry

        entry.lines
            .map { it.accountId }
            .distinct()
            .forEach { accountId ->
                entriesByAccount.computeIfAbsent(accountId) { CopyOnWriteArrayList() } += entry
            }
    }

    override fun all(cursor: Cursor<EntryId>) = entriesById.values.toList().pageFrom(cursor)

    override fun linesOf(id: AccountId, until: LocalDate) =
        entriesByAccount[id].orEmpty()
            .filter { it.occurredOn <= until }
            .flatMap { entry -> entry.lines.filter { it.accountId == id } }

    override fun byAccount(id: AccountId, cursor: Cursor<EntryId>) =
        entriesByAccount[id].orEmpty().pageFrom(cursor)
}
