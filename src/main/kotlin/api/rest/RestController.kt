package altak.api.rest

import io.ktor.server.routing.Route

fun interface RestController {
    fun Route.register()
}
