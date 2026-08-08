package altak.ledger.domain.journal

import altak.ledger.domain.account.Account
import altak.ledger.domain.account.AccountId

data class Posting(val entry: JournalEntry, val accounts: List<Account>) {

    fun accountOf(id: AccountId) = accounts.first { it.id == id }
}

interface PostingStore {

    fun store(posting: Posting)
}
