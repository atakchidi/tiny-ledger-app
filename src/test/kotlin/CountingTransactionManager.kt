package altak.ledger

import altak.ledger.domain.TransactionManager

class CountingTransactionManager : TransactionManager {

    var transactions = 0
        private set

    override fun <T> invoke(work: () -> T): T {
        transactions++
        return work()
    }
}
