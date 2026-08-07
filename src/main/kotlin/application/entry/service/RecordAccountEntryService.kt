package altak.ledger.application.entry.service

import altak.ledger.application.account.AccountNotFound
import altak.ledger.application.account.byIdOrReference
import altak.ledger.application.entry.MovementDto
import altak.ledger.application.entry.ViewEntryDto
import altak.ledger.application.entry.toViewDto
import altak.ledger.domain.Money
import altak.ledger.domain.TransactionManager
import altak.ledger.domain.account.AccountRepository
import altak.ledger.domain.entry.PostingFactory
import altak.ledger.domain.entry.PostingStore
import java.util.Currency

data class RecordAccountEntry(val accountId: String, val movement: MovementDto)

private val RecordAccountEntry.type get() = movement.type
private val RecordAccountEntry.description get() = movement.description
private fun RecordAccountEntry.amountIn(currency: Currency) = Money.of(movement.amount, currency)

class RecordAccountEntryService(
    private val accounts: AccountRepository,
    private val postings: PostingFactory,
    private val store: PostingStore,
    private val transactions: TransactionManager,
) {
    fun execute(command: RecordAccountEntry): ViewEntryDto = transactions {
        with(command) {
            val holder = accounts.byIdOrReference(accountId) ?: throw AccountNotFound(accountId)
            val posting = postings.create(holder, type, amountIn(holder.currency), description)

            store.store(posting)

            posting.entry.toViewDto()
        }
    }
}
