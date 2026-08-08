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
import kotlinx.datetime.LocalDate
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

abstract class EnumSerializer<T : Enum<T>>(
    private val choices: List<T>,
    private val delegate: KSerializer<T>,
) : KSerializer<T> {

    override val descriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: T) = delegate.serialize(encoder, value)

    override fun deserialize(decoder: Decoder): T =
        decoder.literal().let { literal ->
            choices.firstOrNull { it.name == literal }
                ?: throw MalformedValue(literal, "one of ${choices.joinToString { it.name }}")
        }
}

object LocalDateSerializer : KSerializer<LocalDate> {

    override val descriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): LocalDate =
        decoder.literal().let { literal ->
            try {
                LocalDate.parse(literal)
            } catch (notADate: IllegalArgumentException) {
                throw MalformedValue(literal, "a date, as YYYY-MM-DD")
            }
        }
}

private fun Decoder.literal(): String =
    when (this) {
        is JsonDecoder -> decodeJsonElement().jsonPrimitive.content
        else -> decodeString()
    }
