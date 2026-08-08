package altak.ledger.domain.account

import altak.ledger.domain.Cursor
import altak.ledger.domain.Page

interface AccountRepository {

    fun save(account: Account)

    fun byId(id: AccountId): Account?

    fun byIds(ids: Collection<AccountId>): List<Account>

    fun byReference(reference: AccountReference): Account?

    fun all(cursor: Cursor<AccountId>): Page<Account>
}
