package altak.ledger.infrastructure.persistence

import altak.ledger.domain.Cursor
import altak.ledger.domain.Page
import altak.ledger.domain.account.AccountId
import altak.ledger.domain.journal.EntryId
import altak.ledger.domain.journal.EntryLine
import altak.ledger.domain.journal.JournalEntry
import altak.ledger.domain.journal.JournalEntryRepository
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Instant
import java.util.concurrent.CopyOnWriteArrayList

class InMemoryJournalEntryRepository : JournalEntryRepository {

    private val entriesByAccount = ConcurrentHashMap<AccountId, CopyOnWriteArrayList<JournalEntry>>()

    override fun save(entry: JournalEntry) {
        entry.lines
            .map { it.accountId }
            .distinct()
            .forEach { accountId ->
                entriesByAccount.computeIfAbsent(accountId) { CopyOnWriteArrayList() } += entry
            }
    }

    override fun linesOf(id: AccountId, until: Instant): List<EntryLine> =
        entriesByAccount[id].orEmpty()
            .filter { it.createdAt <= until }
            .flatMap { entry -> entry.lines.filter { it.accountId == id } }

    override fun byAccount(id: AccountId, cursor: Cursor<EntryId>): Page<JournalEntry> =
        entriesByAccount[id].orEmpty().pageFrom(cursor)
}
