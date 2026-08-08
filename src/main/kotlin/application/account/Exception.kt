package altak.ledger.application.account

import altak.ledger.application.shared.StatusCode
import altak.ledger.application.shared.UseCaseException

@StatusCode(404)
class AccountNotFound(id: String) : UseCaseException("Account by id '$id' not found.")

@StatusCode(409)
class AccountAlreadyOpen(reference: String) : UseCaseException("Account by reference '$reference' is already open.")
