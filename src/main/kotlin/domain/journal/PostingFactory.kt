package altak.ledger.domain.journal

import altak.ledger.domain.Money
import altak.ledger.domain.account.Account
import altak.ledger.domain.account.AccountRole
import altak.ledger.domain.account.ChartOfAccounts
import altak.ledger.domain.account.Effect
import kotlinx.datetime.LocalDate
import kotlin.time.Clock

enum class MovementType(val counterpart: AccountRole, val effect: Effect, val description: String) {
    DEPOSIT(AccountRole.CASH, Effect.INCREASE, "Deposit"),
    WITHDRAWAL(AccountRole.CASH, Effect.DECREASE, "Withdrawal"),
}

class PostingFactory(
    private val chart: ChartOfAccounts,
    private val entries: JournalEntryFactory,
    private val clock: Clock,
) {
    fun create(
        subject: Account,
        movement: MovementType,
        amount: Money,
        description: String? = null,
        occurredOn: LocalDate? = null,
    ): Posting {
        val moved = subject.move(movement, amount, chart, clock)

        val entry = entries.create(
            description = description ?: movement.description,
            lines = moved.lines,
            occurredOn = occurredOn,
        )

        return Posting(
            entry = entry,
            accounts = moved.accounts
        )
    }
}
