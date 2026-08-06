package altak.ledger.api.rest

import io.ktor.server.routing.Route

fun interface RestController {
    fun Route.register()
}
