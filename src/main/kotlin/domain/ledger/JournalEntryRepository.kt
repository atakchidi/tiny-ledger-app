package altak.ledger.domain.ledger

import altak.ledger.domain.LedgerException
import altak.ledger.domain.account.AccountId

data class Page(val after: EntryId? = null, val limit: Int = DEFAULT_LIMIT) {
    init {
        if (limit !in 1..MAX_LIMIT) {
            throw LedgerException.InvalidPage("A page holds between 1 and $MAX_LIMIT entries, but asked for $limit")
        }
    }

    companion object {
        const val DEFAULT_LIMIT = 50
        const val MAX_LIMIT = 200
    }
}

interface JournalEntryRepository {

    fun save(entry: JournalEntry)

    fun byAccount(id: AccountId, page: Page): List<JournalEntry>
}
