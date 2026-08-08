package altak.ledger.infrastructure.ktor.plugins

import altak.ledger.api.rest.controller.RestController
import altak.ledger.api.rest.controller.accountController
import altak.ledger.api.rest.controller.journalController
import altak.ledger.application.account.service.ListAccountsService
import altak.ledger.application.account.service.OpenAccountService
import altak.ledger.application.account.service.ViewAccountService
import altak.ledger.application.journal.service.ListBalancesService
import altak.ledger.application.journal.service.RecordAccountEntryService
import altak.ledger.application.journal.service.ListAccountEntriesService
import altak.ledger.domain.IdGenerator
import altak.ledger.domain.LedgerCalendar
import altak.ledger.domain.TransactionManager
import altak.ledger.domain.account.AccountFactory
import altak.ledger.domain.account.AccountRepository
import altak.ledger.domain.account.ChartOfAccounts
import altak.ledger.domain.journal.JournalEntryFactory
import altak.ledger.domain.journal.JournalEntryRepository
import altak.ledger.domain.journal.BalancesCalculator
import altak.ledger.domain.journal.PostingFactory
import altak.ledger.domain.journal.PostingStore
import altak.ledger.infrastructure.UuidV7Generator
import altak.ledger.infrastructure.persistence.InMemoryAccountRepository
import altak.ledger.infrastructure.persistence.InMemoryJournalEntryRepository
import altak.ledger.infrastructure.persistence.RepositoryChartOfAccounts
import altak.ledger.infrastructure.persistence.RepositoryPostingStore
import altak.ledger.infrastructure.persistence.InMemoryTransactionManager
import jakarta.validation.Validation
import jakarta.validation.Validator
import jakarta.validation.ValidatorFactory
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

fun Application.configureDependencyInjection() {
    dependencies {
        provide<Clock> { Clock.System }
        provide<TimeZone> { TimeZone.currentSystemDefault() }
        provide(::LedgerCalendar)
        provide<IdGenerator>(::UuidV7Generator)
        provide<ValidatorFactory> { Validation.buildDefaultValidatorFactory() }
        provide<Validator> { resolve<ValidatorFactory>().validator }
        provide<AccountRepository> { InMemoryAccountRepository() }
        provide<JournalEntryRepository> { InMemoryJournalEntryRepository() }
        provide<TransactionManager> { InMemoryTransactionManager() }
        provide(::AccountFactory)
        provide(::JournalEntryFactory)
        provide<ChartOfAccounts>(::RepositoryChartOfAccounts)
        provide<PostingStore>(::RepositoryPostingStore)
        provide(::PostingFactory)
        provide(::BalancesCalculator)

        provide(::OpenAccountService)
        provide(::ListAccountsService)
        provide(::ViewAccountService)
        provide(::ListBalancesService)
        provide(::ListAccountEntriesService)
        provide(::RecordAccountEntryService)

        provide<List<RestController>> { listOf(accountController, journalController) }
    }
}
