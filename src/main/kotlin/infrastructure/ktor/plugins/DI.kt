package altak.infrastructure.ktor.plugins

import altak.api.rest.RestController
import altak.api.rest.accounts.accountController
import altak.api.rest.greeting.greetingController
import altak.application.service.GreetingService
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
