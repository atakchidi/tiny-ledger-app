package altak.ledger.infrastructure.persistence

import altak.ledger.domain.account.Account
import altak.ledger.domain.account.AccountId
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

    override fun cashIn(currency: Currency): Account? =
        accountsById.values.find { it.type == AccountType.ASSET && it.currency == currency }

    override fun all(): List<Account> = accountsById.values.sortedBy { it.id.toString() }
}
