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

private fun Decoder.literal(): String =
    when (this) {
        is JsonDecoder -> decodeJsonElement().jsonPrimitive.content
        else -> decodeString()
    }
