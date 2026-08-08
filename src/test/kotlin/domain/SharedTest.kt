package altak.ledger.domain

import java.math.BigDecimal
import java.util.Currency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
        assertEquals(Money(1050, eur), Money(BigDecimal("10.50"), eur))
        assertEquals(Money(1000, eur), Money(BigDecimal("10"), eur))
        assertEquals(Money(1000, jpy), Money(BigDecimal("1000"), jpy))
    }

    @Test
    fun `hands an amount back in the currency's own precision`() {
        assertEquals(BigDecimal("10.50"), Money(1050, eur).toDecimal())
        assertEquals(BigDecimal("1000"), Money(1000, jpy).toDecimal())
    }

    @Test
    fun `takes an amount whose trailing zeros are finer than the currency, since nothing is lost`() {
        assertEquals(Money(1000, jpy), Money(BigDecimal("1000.00"), jpy))
        assertEquals(Money(1050, eur), Money(BigDecimal("10.500"), eur))
    }

    @Test
    fun `refuses an amount only when a digit would be lost`() {
        assertTrue(Money.fits(BigDecimal("1000.00"), jpy))
        assertTrue(Money.fits(BigDecimal("10.500"), eur))
        assertFalse(Money.fits(BigDecimal("1000.05"), jpy))
        assertFalse(Money.fits(BigDecimal("10.505"), eur))
    }

    @Test
    fun `holds an amount larger than a machine word`() {
        val beyondALong = BigDecimal("99999999999999999999.99")

        assertEquals(beyondALong, Money(beyondALong, eur).toDecimal())
        assertEquals(
            Money(BigDecimal("199999999999999999999.98"), eur),
            Money(beyondALong, eur) + Money(beyondALong, eur),
        )
    }

    @Test
    fun `a cursor holds a sensible number of records`() {
        assertEquals(Cursor.MAX_LIMIT, Cursor<String>(Cursor.MAX_LIMIT).limit)
    }
}
