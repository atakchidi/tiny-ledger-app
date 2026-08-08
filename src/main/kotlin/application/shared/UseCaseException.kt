package altak.ledger.application.shared

/**
 * A refusal a caller can act on, as opposed to a fault of this code. Only these reach the caller
 * in their own words, and each names the answer it deserves with [StatusCode].
 */
abstract class UseCaseException(message: String) : RuntimeException(message)
