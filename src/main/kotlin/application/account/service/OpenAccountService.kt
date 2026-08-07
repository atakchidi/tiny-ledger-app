package altak.ledger.application.account.service

import altak.ledger.application.account.AccountAlreadyOpen
import altak.ledger.application.account.OpenAccountDto
import altak.ledger.application.account.ViewAccountDto
import altak.ledger.application.account.toViewDto
import altak.ledger.domain.TransactionManager
import altak.ledger.domain.account.Account
import altak.ledger.domain.account.AccountReference.Companion.normalized
import altak.ledger.domain.account.AccountRepository
import kotlin.time.Clock

private val OpenAccountDto.holderReference get() = reference?.let(::normalized)

class OpenAccountService(
    private val accounts: AccountRepository,
    private val transactions: TransactionManager,
    private val clock: Clock,
) {
    fun execute(command: OpenAccountDto): ViewAccountDto = transactions {
        with(command) {
            holderReference
                ?.let { accounts.byReference(it) }
                ?.also { throw AccountAlreadyOpen(it.reference.toString()) }

            Account.forHolder(name, currency, clock, holderReference)
                .also(accounts::save)
                .toViewDto()
        }
    }
}
