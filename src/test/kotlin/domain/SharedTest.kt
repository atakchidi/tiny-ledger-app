package altak.ledger.domain

import java.math.BigDecimal
import java.util.Currency
import kotlin.test.Test
import kotlin.test.assertEquals

class SharedTest {

    private val eur = Currency.getInstance("EUR")
    private val usd = Currency.getInstance("USD")
    private val jpy = Currency.getInstance("JPY")

    @Test
    fun `adds amounts of the same currency`() {
        assertEquals(Money(300, eur), Money(100, eur) + Money(200, eur))
    }

    @Test
    fun `adds a negated amount to fall below zero`() {
        assertEquals(Money(-50, eur), Money(100, eur) + -Money(150, eur))
    }

    @Test
    fun `takes a decimal amount into the currency's minor units`() {
        assertEquals(Money(1050, eur), Money.of(BigDecimal("10.50"), eur))
        assertEquals(Money(1000, eur), Money.of(BigDecimal("10"), eur))
        assertEquals(Money(1000, jpy), Money.of(BigDecimal("1000"), jpy))
    }

    @Test
    fun `hands an amount back in the currency's own precision`() {
        assertEquals(BigDecimal("10.50"), Money(1050, eur).toDecimal())
        assertEquals(BigDecimal("1000"), Money(1000, jpy).toDecimal())
    }

    @Test
    fun `a cursor holds a sensible number of records`() {
        assertEquals(Cursor.MAX_LIMIT, Cursor<String>(Cursor.MAX_LIMIT).limit)
    }
}
