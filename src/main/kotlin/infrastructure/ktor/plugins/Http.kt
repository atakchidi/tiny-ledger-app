package altak.ledger.infrastructure.ktor.plugins

import io.ktor.http.*
import io.ktor.openapi.OpenApiDoc
import io.ktor.openapi.OpenApiInfo
import io.ktor.server.application.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.OpenApiDocSource
import io.ktor.server.routing.openapi.hide
import io.ktor.utils.io.ExperimentalKtorApi

private const val MINIMUM_COMPRESSED_BYTES = 1024L

// Generated from the live routing tree, so every route a RestController registers is documented
// without being listed anywhere.
private val openApiSource = OpenApiDocSource.Routing(contentType = ContentType.Application.Json)

private val openApiBaseDoc = OpenApiDoc.build {
    info = OpenApiInfo(title = "ledger-app", version = "1.0.0")
}

@OptIn(ExperimentalKtorApi::class)
fun Application.configureHttp() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
    }
    // Priority is weighed against the quality the client asked with, so deflate wins whenever a
    // client accepts both, and identity is left as the answer when it accepts neither.
    install(Compression) {
        deflate { priority = 10.0 }
        gzip { priority = 1.0 }
        identity { priority = 0.1 }
        minimumSize(MINIMUM_COMPRESSED_BYTES)
    }
    routing {
        swaggerUI(path = "swagger") {
            source = openApiSource
            info = openApiBaseDoc.info
        }

        get("/") {
            call.respondRedirect("/swagger")
        }.hide()

        get("/openapi.json") {
            call.respondText(
                text = openApiSource.read(call.application, openApiBaseDoc).content,
                contentType = ContentType.Application.Json,
            )
        }.hide()
    }
}
