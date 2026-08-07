package altak.ledger.application.shared

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.util.Currency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SerializationTest {

    @Serializable
    private data class Sample(
        @Serializable(with = BigDecimalSerializer::class)
        val amount: BigDecimal,
        @Serializable(with = CurrencySerializer::class)
        val currency: Currency,
    )

    private fun read(json: String) = Json.decodeFromString<Sample>(json)

    @Test
    fun `writes an amount as a number, keeping the scale it was given`() {
        val json = Json.encodeToString(Sample(BigDecimal("10.50"), Currency.getInstance("EUR")))

        assertEquals("""{"amount":10.50,"currency":"EUR"}""", json)
    }

    @Test
    fun `reads an amount written as a number or as a string`() {
        assertEquals(BigDecimal("10.50"), read("""{"amount":10.50,"currency":"EUR"}""").amount)
        assertEquals(BigDecimal("10.50"), read("""{"amount":"10.50","currency":"EUR"}""").amount)
    }

    @Test
    fun `reads a currency by its code`() {
        assertEquals(Currency.getInstance("JPY"), read("""{"amount":1,"currency":"JPY"}""").currency)
    }

    @Test
    fun `says which value it could not read`() {
        val notANumber = assertFailsWith<MalformedValue> { read("""{"amount":"ten","currency":"EUR"}""") }
        val notACurrency = assertFailsWith<MalformedValue> { read("""{"amount":1,"currency":"eur"}""") }

        assertEquals("\"ten\" is not a decimal number", notANumber.message)
        assertEquals("\"eur\" is not an ISO 4217 currency code", notACurrency.message)
    }
}
