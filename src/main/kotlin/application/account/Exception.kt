package altak.ledger.application.account

class AccountNotFound(id: String) : RuntimeException("Account by id '$id' not found.")
