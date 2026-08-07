package altak.ledger.infrastructure.ktor.plugins

import altak.ledger.api.rest.RestController
import altak.ledger.api.rest.accounts.accountController
import altak.ledger.api.rest.ledger.ledgerController
import altak.ledger.application.account.service.ListAccountsService
import altak.ledger.application.account.service.OpenAccountService
import altak.ledger.application.account.service.ViewAccountService
import altak.ledger.application.account.service.ViewBalanceService
import altak.ledger.application.ledger.service.DepositService
import altak.ledger.application.ledger.service.ViewHistoryService
import altak.ledger.application.ledger.service.WithdrawService
import altak.ledger.domain.TransactionManager
import altak.ledger.domain.account.AccountRepository
import altak.ledger.domain.ledger.JournalEntryRepository
import altak.ledger.infrastructure.persistence.InMemoryAccountRepository
import altak.ledger.infrastructure.persistence.InMemoryJournalEntryRepository
import altak.ledger.infrastructure.persistence.InMemoryTransactionManager
import jakarta.validation.Validation
import jakarta.validation.Validator
import jakarta.validation.ValidatorFactory
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import kotlin.time.Clock

fun Application.configureDependencyInjection() {
    dependencies {
        provide<Clock> { Clock.System }
        provide<ValidatorFactory> { Validation.buildDefaultValidatorFactory() }
        provide<Validator> { resolve<ValidatorFactory>().validator }
        provide<AccountRepository> { InMemoryAccountRepository() }
        provide<JournalEntryRepository> { InMemoryJournalEntryRepository() }
        provide<TransactionManager> { InMemoryTransactionManager() }

        provide<OpenAccountService> { OpenAccountService(resolve(), resolve(), resolve()) }
        provide<ListAccountsService> { ListAccountsService(resolve(), resolve()) }
        provide<ViewAccountService> { ViewAccountService(resolve(), resolve()) }
        provide<ViewBalanceService> { ViewBalanceService(resolve(), resolve()) }
        provide<ViewHistoryService> { ViewHistoryService(resolve(), resolve(), resolve()) }
        provide<DepositService> { DepositService(resolve(), resolve(), resolve(), resolve()) }
        provide<WithdrawService> { WithdrawService(resolve(), resolve(), resolve(), resolve()) }

        provide<List<RestController>> { listOf(accountController, ledgerController) }
    }
}
