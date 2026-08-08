package altak.ledger.application.account

import altak.ledger.domain.account.Account
import altak.ledger.domain.account.AccountId
import altak.ledger.domain.account.AccountReference
import altak.ledger.domain.account.AccountRepository

fun AccountRepository.find(idOrReference: String): Account =
    byIdOrReference(idOrReference) ?: throw AccountNotFound(idOrReference)

private fun AccountRepository.byIdOrReference(raw: String): Account? =
    raw.toAccountIdOrNull()?.let(::byId) ?: raw.toReferenceOrNull()?.let(::byReference)

private fun String.toAccountIdOrNull() =
    try {
        AccountId(this)
    } catch (notAnId: IllegalArgumentException) {
        null
    }

private fun String.toReferenceOrNull() =
    try {
        AccountReference(this)
    } catch (notAReference: IllegalArgumentException) {
        null
    }
