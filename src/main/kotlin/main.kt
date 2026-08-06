package altak

import altak.infrastructure.ktor.rootModule
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer

private const val DEFAULT_PORT = 8080

// Fails loudly on a malformed PORT rather than silently binding the default, which would
// otherwise surface as a service that is up but unreachable.
private fun resolvePort(): Int {
    val configuredPort = System.getenv("PORT")
    if (configuredPort.isNullOrBlank()) return DEFAULT_PORT

    return configuredPort.toIntOrNull()
        ?: error("PORT must be a number, but was \"$configuredPort\"")
}

fun main(args: Array<String>) {
    embeddedServer(
        factory = io.ktor.server.netty.Netty,
        port = resolvePort(),
        host = "0.0.0.0",
        module = Application::rootModule
    ).start(wait = true)
}
