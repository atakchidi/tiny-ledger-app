package altak.ledger.domain.account

import altak.ledger.domain.Cursor
import java.util.Currency

interface AccountRepository {

    fun save(account: Account)

    fun byId(id: AccountId): Account?

    fun byReference(reference: AccountReference): Account?

    fun cashIn(currency: Currency): Account?

    fun all(cursor: Cursor<AccountId>): List<Account>
}
