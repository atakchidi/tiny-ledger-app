package altak.ledger.infrastructure.persistence

import altak.ledger.domain.account.AccountRepository
import altak.ledger.domain.entry.JournalEntryRepository
import altak.ledger.domain.entry.Posting
import altak.ledger.domain.entry.PostingStore

class RepositoryPostingStore(
    private val accounts: AccountRepository,
    private val entries: JournalEntryRepository,
) : PostingStore {

    override fun store(posting: Posting) {
        posting.accounts.forEach(accounts::save)
        entries.save(posting.entry)
    }
}
