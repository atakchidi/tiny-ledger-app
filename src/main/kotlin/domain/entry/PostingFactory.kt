package altak.ledger.domain.entry

import altak.ledger.domain.Money
import altak.ledger.domain.account.Account
import altak.ledger.domain.account.AccountRole
import altak.ledger.domain.account.ChartOfAccounts
import altak.ledger.domain.account.Effect
import kotlin.time.Clock

enum class MovementType(val counterpart: AccountRole, val effect: Effect, val description: String) {
    DEPOSIT(AccountRole.CASH, Effect.INCREASE, "Deposit"),
    WITHDRAWAL(AccountRole.CASH, Effect.DECREASE, "Withdrawal"),
}

class PostingFactory(
    private val chart: ChartOfAccounts,
    private val clock: Clock,
) {
    fun create(subject: Account, movement: MovementType, amount: Money, description: String? = null): Posting {
        val counterpart = chart.of(movement.counterpart, subject.currency)
        val side = subject.sideFor(movement.effect)

        val entry = JournalEntry(
            description = description ?: movement.description,
            lines = listOf(subject.line(side, amount), counterpart.line(side.opposite, amount)),
            clock = clock,
        )

        return Posting(
            entry = entry,
            accounts = listOf(subject, counterpart).map { account ->
                entry.lines.filter { it.accountId == account.id }
                    .fold(account) { projected, line -> projected.project(line, clock) }
            },
        )
    }
}
