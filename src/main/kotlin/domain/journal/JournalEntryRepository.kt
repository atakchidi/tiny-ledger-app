package altak.ledger.domain.journal

import altak.ledger.domain.Cursor
import altak.ledger.domain.Page
import altak.ledger.domain.account.AccountId
import kotlin.time.Instant

interface JournalEntryRepository {

    fun save(entry: JournalEntry)

    fun byAccount(id: AccountId, cursor: Cursor<EntryId>): Page<JournalEntry>

    fun linesOf(id: AccountId, until: Instant): List<EntryLine>
}
