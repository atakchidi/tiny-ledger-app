package altak.ledger.domain.account

import java.util.Currency

interface AccountRepository {

    fun save(account: Account)

    fun byId(id: AccountId): Account?

    fun cashIn(currency: Currency): Account?

    fun all(): List<Account>
}
