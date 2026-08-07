package altak.ledger.api.rest.controller

import io.ktor.server.routing.Route

fun interface RestController {
    fun Route.register()
}
