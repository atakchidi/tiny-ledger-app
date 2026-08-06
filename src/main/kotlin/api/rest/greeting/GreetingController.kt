@file:OptIn(ExperimentalKtorApi::class)

package altak.api.rest.greeting

import altak.api.rest.RestController
import altak.api.rest.inject
import altak.api.rest.schemaOf
import altak.application.service.GreetingService
import io.ktor.http.*
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.utils.io.ExperimentalKtorApi

val greetingController = RestController {
    get("/greeting") {
        val greetingService = inject<GreetingService>()

        call.respondText(greetingService.sayHello())
    }.describe {
        summary = "Greeting"
        tag("greeting")

        responses {
            response(HttpStatusCode.OK.value) {
                description = "A plain-text greeting"
                ContentType.Text.Plain { schema = schemaOf<String>() }
            }
        }
    }

    get("/json/kotlinx-serialization") {
        call.respond(mapOf("hello" to "world"))
    }.describe {
        summary = "JSON serialization sample"
        tag("greeting")

        responses {
            response(HttpStatusCode.OK.value) {
                description = "A sample JSON object"
                schema = schemaOf<Map<String, String>>()
            }
        }
    }
}
