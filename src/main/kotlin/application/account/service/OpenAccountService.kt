package altak.ledger.application.account.service

import altak.ledger.application.account.OpenAccountDto
import altak.ledger.application.account.ViewAccountDto
import altak.ledger.application.account.toViewDto
import altak.ledger.domain.TransactionManager
import altak.ledger.domain.account.Account
import altak.ledger.domain.account.AccountRepository
import altak.ledger.domain.currencyOf
import kotlin.time.Clock

private val OpenAccountDto.holderCurrency get() = currencyOf(currency)

class OpenAccountService(
    private val accounts: AccountRepository,
    private val transaction: TransactionManager,
    private val clock: Clock,
) {

    fun execute(command: OpenAccountDto): ViewAccountDto = transaction {
        with(command) {
            Account.forHolder(name, holderCurrency, clock)
                .also(accounts::save)
                .toViewDto()
        }
    }
}
