package altak.ledger.infrastructure.persistence

import altak.ledger.domain.account.AccountId
import altak.ledger.domain.ledger.JournalEntry
import altak.ledger.domain.ledger.JournalEntryRepository
import altak.ledger.domain.ledger.Page
import java.util.concurrent.ConcurrentHashMap
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

    override fun byAccount(id: AccountId, page: Page): List<JournalEntry> {
        val recorded = entriesByAccount[id] ?: return emptyList()
        val cursor = page.after ?: return recorded.take(page.limit)
        val position = recorded.indexOfFirst { it.id == cursor }
        return if (position < 0) emptyList() else recorded.drop(position + 1).take(page.limit)
    }
}
