package altak.ledger.application.account.service

import altak.ledger.application.account.AccountAlreadyOpen
import altak.ledger.application.account.OpenAccountDto
import altak.ledger.application.account.ViewAccountDto
import altak.ledger.application.account.toViewDto
import altak.ledger.domain.IdGenerator
import altak.ledger.domain.TransactionManager
import altak.ledger.domain.account.Account
import altak.ledger.domain.account.AccountReference
import altak.ledger.domain.account.AccountRepository
import kotlin.time.Clock

data class OpenAccount(val data: OpenAccountDto)
private val OpenAccountDto.holderReference get() = AccountReference(reference)

class OpenAccountService(
    private val accounts: AccountRepository,
    private val ids: IdGenerator,
    private val transactions: TransactionManager,
    private val clock: Clock,
) {
    fun execute(command: OpenAccount) = transactions {
        with(command.data) {
            accounts.byReference(holderReference)?.also { throw AccountAlreadyOpen(it.reference.toString()) }

            Account.forHolder(name, currency, ids, clock, holderReference)
                .also(accounts::save)
                .toViewDto()
        }
    }
}
