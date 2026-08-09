package altak.ledger.api.rest

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * `testApplication` starts with an empty config rather than the one on the classpath, so the file is
 * named here. Loading it — instead of installing the module by hand — keeps a test on the same wiring
 * the server boots with: `src/test/resources/application.conf` shadows the deployed one, adds
 * `testDependencies` after `rootModule`, and blanks the seed, so a test starts on empty books.
 * Blanking rather than omitting is deliberate — HOCON merges the two files, so an absent key here
 * would still resolve to the deployed seed.
 */
fun apiTest(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
    configure("application.conf")

    block()
}

suspend fun HttpResponse.id(): String = field("id")

suspend fun HttpResponse.field(name: String): String = data().getValue(name).jsonPrimitive.content

suspend fun HttpResponse.data(): JsonObject = envelope().getValue("data").jsonObject

suspend fun HttpResponse.records(): JsonArray = envelope().getValue("data").jsonArray

suspend fun HttpResponse.nextCursor(): String? =
    envelope()["nextCursor"]?.jsonPrimitive?.content?.takeIf { it != "null" }

suspend fun HttpResponse.errors(): List<String> =
    envelope().getValue("errors").jsonArray.map { it.jsonPrimitive.content }

suspend fun HttpResponse.envelope(): JsonObject = Json.parseToJsonElement(bodyAsText()).jsonObject
