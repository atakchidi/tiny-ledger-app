package altak.ledger.domain.journal

import altak.ledger.domain.account.Account

data class Posting(val entry: JournalEntry, val accounts: List<Account>)

interface PostingStore {

    fun store(posting: Posting)
}
