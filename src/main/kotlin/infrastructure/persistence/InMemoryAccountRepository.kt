package altak.ledger.infrastructure.persistence

import altak.ledger.domain.Cursor
import altak.ledger.domain.account.Account
import altak.ledger.domain.account.AccountId
import altak.ledger.domain.account.AccountReference
import altak.ledger.domain.account.AccountRepository
import altak.ledger.domain.account.AccountType
import java.util.Currency
import java.util.concurrent.ConcurrentHashMap

class InMemoryAccountRepository : AccountRepository {

    private val accountsById = ConcurrentHashMap<AccountId, Account>()

    override fun save(account: Account) {
        accountsById[account.id] = account
    }

    override fun byId(id: AccountId): Account? = accountsById[id]

    override fun byReference(reference: AccountReference): Account? =
        accountsById.values.find { it.reference == reference }

    override fun cashIn(currency: Currency): Account? =
        accountsById.values.find { it.type == AccountType.ASSET && it.currency == currency }

    override fun all(cursor: Cursor<AccountId>): List<Account> {
        val opened = accountsById.values.sortedBy { it.id.toString() }
        val after = cursor.after ?: return opened.take(cursor.limit)
        val position = opened.indexOfFirst { it.id == after }
        return if (position < 0) emptyList() else opened.drop(position + 1).take(cursor.limit)
    }
}
