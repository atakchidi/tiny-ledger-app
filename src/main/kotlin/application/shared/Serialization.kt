package altak.ledger.application.shared

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigDecimal
import java.util.Currency
import kotlin.time.Instant
import kotlin.uuid.Uuid

class MalformedValue(value: String, expected: String) :
    SerializationException("\"$value\" is not $expected")

object BigDecimalSerializer : KSerializer<BigDecimal> {

    override val descriptor = PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BigDecimal) =
        when (encoder) {
            is JsonEncoder -> encoder.encodeJsonElement(JsonUnquotedLiteral(value.toPlainString()))
            else -> encoder.encodeString(value.toPlainString())
        }

    override fun deserialize(decoder: Decoder): BigDecimal =
        decoder.literal().let { literal ->
            try {
                BigDecimal(literal)
            } catch (notANumber: NumberFormatException) {
                throw MalformedValue(literal, "a decimal number")
            }
        }
}

object CurrencySerializer : KSerializer<Currency> {

    override val descriptor = PrimitiveSerialDescriptor("Currency", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Currency) = encoder.encodeString(value.currencyCode)

    override fun deserialize(decoder: Decoder): Currency =
        decoder.literal().let { literal ->
            try {
                Currency.getInstance(literal)
            } catch (notACurrency: IllegalArgumentException) {
                throw MalformedValue(literal, "an ISO 4217 currency code")
            }
        }
}

object UuidSerializer : KSerializer<Uuid> {

    override val descriptor = PrimitiveSerialDescriptor("Uuid", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Uuid) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): Uuid =
        decoder.literal().let { literal ->
            try {
                Uuid.parse(literal)
            } catch (notAnId: IllegalArgumentException) {
                throw MalformedValue(literal, "an identifier")
            }
        }
}

object InstantSerializer : KSerializer<Instant> {

    override val descriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): Instant =
        decoder.literal().let { literal ->
            try {
                Instant.parse(literal)
            } catch (notAMoment: IllegalArgumentException) {
                throw MalformedValue(literal, "an ISO-8601 moment")
            }
        }
}

private fun Decoder.literal(): String =
    when (this) {
        is JsonDecoder -> decodeJsonElement().jsonPrimitive.content
        else -> decodeString()
    }
