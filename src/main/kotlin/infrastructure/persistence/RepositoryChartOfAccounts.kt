package altak.ledger.infrastructure.persistence

import altak.ledger.domain.account.Account
import altak.ledger.domain.account.AccountFactory
import altak.ledger.domain.account.AccountRepository
import altak.ledger.domain.account.AccountRole
import altak.ledger.domain.account.ChartOfAccounts
import java.util.Currency

class RepositoryChartOfAccounts(
    private val accounts: AccountRepository,
    private val factory: AccountFactory,
) : ChartOfAccounts {

    override fun of(role: AccountRole, currency: Currency): Account =
        accounts.byReference(role.referenceFor(currency)) ?: factory.internal(role, currency)
}
