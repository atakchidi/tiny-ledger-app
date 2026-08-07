package altak.ledger.api.rest

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

suspend fun HttpResponse.id(): String = field("id")

suspend fun HttpResponse.field(name: String): String =
    Json.parseToJsonElement(bodyAsText()).jsonObject.getValue(name).jsonPrimitive.content
