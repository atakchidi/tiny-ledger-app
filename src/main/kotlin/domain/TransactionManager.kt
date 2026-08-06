package altak.ledger.domain

interface TransactionManager {

    operator fun <T> invoke(work: () -> T): T
}
