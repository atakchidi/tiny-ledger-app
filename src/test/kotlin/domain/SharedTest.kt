package altak.ledger.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SharedTest {

    private val eur = currencyOf("EUR")
    private val usd = currencyOf("USD")

    @Test
    fun `adds amounts of the same currency`() {
        assertEquals(Money(300, eur), Money(100, eur) + Money(200, eur))
    }

    @Test
    fun `adds a negated amount to fall below zero`() {
        assertEquals(Money(-50, eur), Money(100, eur) + -Money(150, eur))
    }

    @Test
    fun `refuses to combine different currencies`() {
        assertFailsWith<LedgerException.CurrencyMismatch> { Money(100, eur) + Money(100, usd) }
    }

    @Test
    fun `rejects an unknown currency code`() {
        assertFailsWith<LedgerException.UnknownCurrency> { currencyOf("eur") }
        assertFailsWith<LedgerException.UnknownCurrency> { currencyOf("XYZ") }
    }
}
