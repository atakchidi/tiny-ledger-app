package altak.ledger.domain.journal

import altak.ledger.domain.Cursor
import altak.ledger.domain.Page
import altak.ledger.domain.account.AccountId
import kotlinx.datetime.LocalDate

interface JournalEntryRepository {

    fun save(entry: JournalEntry)

    fun all(cursor: Cursor<EntryId>): Page<JournalEntry>

    fun byAccount(id: AccountId, cursor: Cursor<EntryId>): Page<JournalEntry>

    fun linesOf(id: AccountId, until: LocalDate): List<EntryLine>
}
