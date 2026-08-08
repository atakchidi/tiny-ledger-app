package altak.ledger.domain.account

import altak.ledger.domain.IdGenerator
import java.util.Currency
import kotlin.time.Clock

class AccountFactory(
    private val ids: IdGenerator,
    private val clock: Clock,
) {
    fun forHolder(name: String, currency: Currency, reference: AccountReference): Account =
        Account(
            id = nextId(),
            reference = reference,
            name = name,
            currency = currency,
            type = AccountType.LIABILITY,
            createdAt = clock.now(),
        )

    fun internal(role: AccountRole, currency: Currency): Account =
        Account(
            id = nextId(),
            reference = role.referenceFor(currency),
            name = "${role.title} ${currency.currencyCode}",
            currency = currency,
            type = role.type,
            createdAt = clock.now(),
        )

    private fun nextId() = AccountId(ids.nextId(clock))
}
