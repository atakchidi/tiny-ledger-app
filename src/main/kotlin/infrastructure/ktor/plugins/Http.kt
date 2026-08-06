package altak.infrastructure.ktor.plugins

import io.ktor.http.*
import io.ktor.openapi.OpenApiDoc
import io.ktor.openapi.OpenApiInfo
import io.ktor.server.application.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.OpenApiDocSource
import io.ktor.server.routing.openapi.hide
import io.ktor.utils.io.ExperimentalKtorApi

// Generated from the live routing tree, so every route a RestController registers is documented
// without being listed anywhere.
private val openApiSource = OpenApiDocSource.Routing(contentType = ContentType.Application.Json)

private val openApiBaseDoc = OpenApiDoc.build {
    info = OpenApiInfo(title = "ledger", version = "1.0.0-SNAPSHOT")
}

@OptIn(ExperimentalKtorApi::class)
fun Application.configureHttp() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader("MyCustomHeader")
        anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
    }
    install(Compression)
    routing {
        openAPI(path = "/") {
            source = openApiSource
            info = openApiBaseDoc.info
        }

        // hide() keeps the spec endpoint itself out of the spec.
        get("/openapi.json") {
            call.respondText(
                text = openApiSource.read(call.application, openApiBaseDoc).content,
                contentType = ContentType.Application.Json,
            )
        }.hide()
    }
}
