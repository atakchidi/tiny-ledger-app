package altak.ledger.domain.journal

import altak.ledger.domain.account.Account
import altak.ledger.domain.account.AccountId
import altak.ledger.domain.account.AccountReference

data class Posting(val entry: JournalEntry, val accounts: List<Account>) {

    fun referenceOf(id: AccountId) = accounts.first { it.id == id }.reference
}

interface PostingStore {

    fun store(posting: Posting)
}
