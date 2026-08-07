package altak.ledger.infrastructure.persistence

import altak.ledger.domain.account.AccountRepository
import altak.ledger.domain.journal.JournalEntryRepository
import altak.ledger.domain.journal.Posting
import altak.ledger.domain.journal.PostingStore

class RepositoryPostingStore(
    private val accounts: AccountRepository,
    private val entries: JournalEntryRepository,
) : PostingStore {

    override fun store(posting: Posting) {
        posting.accounts.forEach(accounts::save)
        entries.save(posting.entry)
    }
}
