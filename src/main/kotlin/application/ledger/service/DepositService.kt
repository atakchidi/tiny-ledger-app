package altak.ledger.application.ledger.service

import altak.ledger.application.account.AccountNotFound
import altak.ledger.application.account.toAccountId
import altak.ledger.application.ledger.MovementDto
import altak.ledger.application.ledger.ViewEntryDto
import altak.ledger.application.ledger.toViewDto
import altak.ledger.application.shared.toMoney
import altak.ledger.domain.TransactionManager
import altak.ledger.domain.account.Account
import altak.ledger.domain.account.AccountRepository
import altak.ledger.domain.ledger.JournalEntryRepository
import altak.ledger.domain.ledger.Ledger
import java.util.Currency
import kotlin.time.Clock

data class Deposit(val accountId: String, val movement: MovementDto)

private val Deposit.id get() = accountId.toAccountId()
private val Deposit.description get() = movement.description
private fun Deposit.amountIn(currency: Currency) = movement.amount.toMoney(currency)

class DepositService(
    private val accounts: AccountRepository,
    private val entries: JournalEntryRepository,
    private val transactions: TransactionManager,
    private val clock: Clock,
) {
    fun execute(command: Deposit): ViewEntryDto = transactions {
        with(command) {
            val holder = accounts.byId(id) ?: throw AccountNotFound(accountId)
            val cash = accounts.cashIn(holder.currency) ?: Account.forCash(holder.currency, clock)

            Ledger(holder, cash, clock)
                .deposit(amountIn(holder.currency), description)
                .also { posting ->
                    posting.accounts.forEach(accounts::save)
                    entries.save(posting.entry)
                }
                .entry
                .toViewDto()
        }
    }
}
