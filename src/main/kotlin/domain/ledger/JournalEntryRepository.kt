package altak.ledger.domain.ledger

import altak.ledger.domain.Cursor
import altak.ledger.domain.account.AccountId

interface JournalEntryRepository {

    fun save(entry: JournalEntry)

    fun byAccount(id: AccountId, cursor: Cursor<EntryId>): List<JournalEntry>
}
