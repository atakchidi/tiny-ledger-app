package altak.api.rest

import io.ktor.openapi.JsonSchema
import io.ktor.openapi.JsonSchemaInference
import kotlin.reflect.typeOf

/**
 * Derives the OpenAPI schema for [T] from its kotlinx-serialization descriptor, so `describe {}`
 * blocks reference DTO types instead of restating their shape.
 */
inline fun <reified T> JsonSchemaInference.schemaOf(): JsonSchema =
    buildSchema(typeOf<T>())
