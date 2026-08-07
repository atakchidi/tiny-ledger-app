package altak.ledger.infrastructure.ktor.plugins

import altak.ledger.api.rest.controller.RestController
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    val controllers: List<RestController> by dependencies

    routing {
        controllers.forEach { controller ->
            with(controller) { register() }
        }
    }
}
