package altak.ledger.application.account

import altak.ledger.domain.account.Account
import altak.ledger.domain.account.AccountId
import altak.ledger.domain.account.AccountReference
import altak.ledger.domain.account.AccountRepository

fun AccountRepository.byIdOrReference(raw: String): Account? =
    raw.toAccountIdOrNull()?.let(::byId) ?: raw.toReferenceOrNull()?.let(::byReference)

private fun String.toAccountIdOrNull(): AccountId? =
    try {
        AccountId(this)
    } catch (notAnId: IllegalArgumentException) {
        null
    }

private fun String.toReferenceOrNull(): AccountReference? =
    try {
        AccountReference(this)
    } catch (notAReference: AccountReference.Malformed) {
        null
    }
