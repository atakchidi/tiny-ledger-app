package altak.ledger.infrastructure.persistence

import altak.ledger.domain.Cursor
import altak.ledger.domain.account.Account
import altak.ledger.domain.account.AccountId
import altak.ledger.domain.account.AccountReference
import altak.ledger.domain.account.AccountRepository
import java.util.concurrent.ConcurrentHashMap

class InMemoryAccountRepository : AccountRepository {

    private val accountsById = ConcurrentHashMap<AccountId, Account>()

    override fun save(account: Account) {
        accountsById[account.id] = account
    }

    override fun byId(id: AccountId) = accountsById[id]

    override fun byIds(ids: Collection<AccountId>) = ids.distinct().mapNotNull { accountsById[it] }

    override fun byReference(reference: AccountReference) =
        accountsById.values.find { it.reference == reference }

    override fun all(cursor: Cursor<AccountId>) =
        accountsById.values.toList().pageFrom(cursor)
}
