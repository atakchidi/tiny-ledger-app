package altak.ledger.infrastructure.ktor.plugins

import altak.ledger.api.rest.RestController
import altak.ledger.api.rest.accounts.accountController
import altak.ledger.api.rest.greeting.greetingController
import altak.ledger.application.service.GreetingService
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*

fun Application.configureDependencyInjection() {
    dependencies {
        provide<GreetingService> { GreetingService { "Hello, World!" } }

        provide<List<RestController>> {
            listOf(
                greetingController,
                accountController,
            )
        }
    }
}
