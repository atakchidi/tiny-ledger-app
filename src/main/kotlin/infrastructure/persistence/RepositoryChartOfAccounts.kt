package altak.ledger.infrastructure.persistence

import altak.ledger.domain.account.Account
import altak.ledger.domain.account.AccountRepository
import altak.ledger.domain.account.AccountRole
import altak.ledger.domain.account.ChartOfAccounts
import java.util.Currency
import kotlin.time.Clock

class RepositoryChartOfAccounts(
    private val accounts: AccountRepository,
    private val clock: Clock,
) : ChartOfAccounts {

    override fun of(role: AccountRole, currency: Currency): Account =
        accounts.byReference(role.referenceFor(currency)) ?: Account.internal(role, currency, clock)
}
