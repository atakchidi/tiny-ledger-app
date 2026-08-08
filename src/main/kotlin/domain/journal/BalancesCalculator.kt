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
    fun calculate(query: BalanceQuery, cursor: Cursor<AccountId>) = with(query) {
        val accounts = accountId
            ?.let { id -> Page(listOfNotNull(accounts.byId(id))) }
            ?: accounts.all(cursor)

        accounts.map { account ->
            Balance(account, onDate, account.balanceAsOf(onDate))
        }
    }

    private fun Account.balanceAsOf(onDate: LocalDate) =
        entries.linesOf(id, onDate).fold(Money.zero(currency)) { running, line ->
            running + line.signedAgainst(type.direction)
        }
}
