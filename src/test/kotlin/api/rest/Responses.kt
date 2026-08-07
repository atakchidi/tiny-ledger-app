package altak.ledger.api.rest

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

suspend fun HttpResponse.id(): String = field("id")

suspend fun HttpResponse.field(name: String): String = data().getValue(name).jsonPrimitive.content

suspend fun HttpResponse.data(): JsonObject = envelope().getValue("data").jsonObject

suspend fun HttpResponse.records(): JsonArray = envelope().getValue("data").jsonArray

suspend fun HttpResponse.nextCursor(): String? =
    envelope()["nextCursor"]?.jsonPrimitive?.content?.takeIf { it != "null" }

suspend fun HttpResponse.envelope(): JsonObject = Json.parseToJsonElement(bodyAsText()).jsonObject
