package altak.ledger.application.journal.service

import altak.ledger.application.account.AccountNotFound
import altak.ledger.application.account.byIdOrReference
import altak.ledger.application.journal.RecordAccountEntryDto
import altak.ledger.application.journal.ViewEntryDto
import altak.ledger.application.journal.toViewDto
import altak.ledger.domain.Money
import altak.ledger.domain.TransactionManager
import altak.ledger.domain.account.AccountRepository
import altak.ledger.domain.journal.PostingFactory
import altak.ledger.domain.journal.PostingStore
import java.util.Currency

data class RecordAccountEntry(val movement: RecordAccountEntryDto)

private val RecordAccountEntry.account get() = movement.account
private val RecordAccountEntry.type get() = movement.type
private val RecordAccountEntry.description get() = movement.description
private val RecordAccountEntry.occurredOn get() = movement.occurredOn
private fun RecordAccountEntry.amountIn(currency: Currency) = Money.of(movement.amount, currency)

class RecordAccountEntryService(
    private val accounts: AccountRepository,
    private val postings: PostingFactory,
    private val store: PostingStore,
    private val transactions: TransactionManager,
) {
    fun execute(command: RecordAccountEntry): ViewEntryDto = transactions {
        with(command) {
            val holder = accounts.byIdOrReference(account) ?: throw AccountNotFound(account)
            val posting = postings.create(holder, type, amountIn(holder.currency), description, occurredOn)

            store.store(posting)

            posting.entry.toViewDto(posting::referenceOf)
        }
    }
}
