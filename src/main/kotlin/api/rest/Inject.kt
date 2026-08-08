package altak.ledger.api.rest

import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.di.resolve
import io.ktor.server.routing.RoutingContext

/**
 * Suspends rather than blocking, and registers no startup requirement — unlike `by dependencies`,
 * which is meant for module scope and would capture a stack trace on every request if used here.
 */
suspend inline fun <reified T> RoutingContext.inject(name: String? = null): T =
    call.application.dependencies.resolve(name)
