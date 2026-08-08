package altak.ledger.domain.journal

import altak.ledger.domain.Cursor
import altak.ledger.domain.Money
import altak.ledger.domain.Page
import altak.ledger.domain.account.Account
import altak.ledger.domain.account.AccountId
import altak.ledger.domain.account.AccountRepository
import kotlinx.datetime.LocalDate

data class BalanceQuery(val onDate: LocalDate, val accountId: AccountId? = null)

data class Balance(val account: Account, val onDate: LocalDate, val amount: Money)

class BalancesCalculator(
    private val accounts: AccountRepository,
    private val entries: JournalEntryRepository,
) {
    fun calculate(query: BalanceQuery, cursor: Cursor<AccountId>): Page<Balance> =
        asked(query.accountId, cursor).map { account ->
            Balance(account, query.onDate, account.balanceAsOf(query.onDate))
        }

    private fun asked(accountId: AccountId?, cursor: Cursor<AccountId>): Page<Account> =
        accountId
            ?.let { id -> Page(listOfNotNull(accounts.byId(id))) }
            ?: accounts.all(cursor)

    private fun Account.balanceAsOf(onDate: LocalDate) =
        entries.linesOf(id, onDate).fold(Money.zero(currency)) { running, line ->
            running + line.signedAgainst(type.normalSide)
        }
}
