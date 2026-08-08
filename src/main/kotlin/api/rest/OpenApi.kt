package altak.ledger.api.rest

import altak.ledger.domain.Cursor
import altak.ledger.domain.Sorting
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.GenericElementString
import io.ktor.openapi.JsonSchema
import io.ktor.openapi.JsonSchemaInference
import io.ktor.openapi.Operation
import io.ktor.openapi.Responses
import kotlin.reflect.typeOf

/**
 * Derives the OpenAPI schema for [T] from its kotlinx-serialization descriptor, so `describe {}`
 * blocks reference DTO types instead of restating their shape.
 */
inline fun <reified T> JsonSchemaInference.schemaOf(): JsonSchema =
    buildSchema(typeOf<T>())

inline fun <reified T> Operation.Builder.accepts() {
    requestBody {
        required = true
        schema = schemaOf<T>()
    }
}

fun Operation.Builder.pages(records: String, sortable: Sortable = Sortable()) {
    parameters {
        query("after") {
            description = "The id of the last ${records.trimEnd('s')} on the previous page"
            schema = schemaOf<String>()
        }
        query("limit") {
            description = "How many $records the page holds, at most ${Cursor.MAX_LIMIT}"
            schema = schemaOf<Int>()
        }
        query("sort") {
            description = "The field to order the $records by"
            schema = schemaOf<String>().copy(enum = sortable.fields.map(::GenericElementString))
        }
        query("direction") {
            description = "Which way to order them"
            schema = schemaOf<Sorting.Direction>()
        }
    }
}

inline fun <reified T> Responses.Builder.answers(status: HttpStatusCode, description: String) {
    response(status.value) {
        this.description = description
        schema = schemaOf<T>()
    }
}

fun Responses.Builder.refuses(status: HttpStatusCode, description: String) {
    response(status.value) {
        this.description = description
        schema = schemaOf<ErrorResponse>()
    }
}
