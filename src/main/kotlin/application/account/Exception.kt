package altak.ledger.application.account

import altak.ledger.application.shared.StatusCode

@StatusCode(404)
class AccountNotFound(id: String) : RuntimeException("Account by id '$id' not found.")

@StatusCode(409)
class AccountAlreadyOpen(reference: String) : RuntimeException("Account by reference '$reference' is already open.")
