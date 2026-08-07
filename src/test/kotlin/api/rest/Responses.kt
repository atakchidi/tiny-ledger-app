package altak.ledger.api.rest

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

suspend fun HttpResponse.id(): String = field("id")

suspend fun HttpResponse.field(name: String): String = data().getValue(name).jsonPrimitive.content

suspend fun HttpResponse.data() =
    Json.parseToJsonElement(bodyAsText()).jsonObject.getValue("data").jsonObject
