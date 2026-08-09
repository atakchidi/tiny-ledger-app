package altak.ledger.application.account.service

import altak.ledger.application.account.AccountAlreadyOpen
import altak.ledger.application.account.OpenAccountDto
import altak.ledger.application.account.toViewDto
import altak.ledger.domain.TransactionManager
import altak.ledger.domain.account.AccountFactory
import altak.ledger.domain.account.AccountReference
import altak.ledger.domain.account.AccountRepository

data class OpenAccount(val data: OpenAccountDto)
private val OpenAccountDto.holderReference get() = AccountReference(reference)

class OpenAccountService(
    private val accounts: AccountRepository,
    private val factory: AccountFactory,
    private val transaction: TransactionManager,
) {
    fun execute(command: OpenAccount) = transaction {
        with(command.data) {
            accounts.byReference(holderReference)?.also { throw AccountAlreadyOpen(it.reference.toString()) }

            factory.forHolder(name, currency, holderReference)
                .also(accounts::save)
                .toViewDto()
        }
    }
}
