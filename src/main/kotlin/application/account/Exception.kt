package altak.ledger.application.account

import altak.ledger.domain.LedgerException

class AccountNotFound(id: String) : LedgerException("Account by id '$id' not found.")

class AccountAlreadyOpen(reference: String) : LedgerException("Account by reference '$reference' is already open.")
