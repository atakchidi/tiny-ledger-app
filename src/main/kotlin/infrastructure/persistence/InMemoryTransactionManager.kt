package altak.ledger.infrastructure.persistence

import altak.ledger.domain.TransactionManager

class InMemoryTransactionManager : TransactionManager {

    override fun <T> invoke(work: () -> T): T = work()
}
