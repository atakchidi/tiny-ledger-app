package altak.Infrastructure.Ktor.plugins

import altak.Application.Service.GreetingService
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*

// The contents of the `install` function will be used for the project template
fun Application.configureDependencyInjection() {
    dependencies {
        provide { GreetingService { "Hello, World!" } }
    }
}
