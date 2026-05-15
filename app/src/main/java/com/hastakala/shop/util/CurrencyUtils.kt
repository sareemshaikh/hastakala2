package com.hastakala.testshop.util

import java.text.NumberFormat
import java.util.Locale

/**
 * Centralised currency formatting for the Hasta-Kala Shop app.
 *
 * All monetary values are displayed in Indian Rupees (₹) using the
 * en_IN locale so that grouping separators and the official ₹ symbol
 * are applied consistently across every screen.
 *
 * Usage:
 *   CurrencyUtils.format(1234.5)  →  "₹1,234.50"
 *   CurrencyUtils.formatCompact(1234.5)  →  "₹1,234"
 */
object CurrencyUtils {

    private val IN_LOCALE = Locale("en", "IN")

    /** Full currency format: ₹1,234.50 */
    fun format(amount: Double): String {
        val nf = NumberFormat.getCurrencyInstance(IN_LOCALE)
        nf.minimumFractionDigits = 2
        nf.maximumFractionDigits = 2
        return nf.format(amount)
    }

    /** Per-unit price label: ₹50.00/unit */
    fun formatPerUnit(unitPrice: Double): String = "${format(unitPrice)}/unit"

    /** Compact format with no decimals for large KPI numbers: ₹1,234 */
    fun formatCompact(amount: Double): String {
        val nf = NumberFormat.getCurrencyInstance(IN_LOCALE)
        nf.minimumFractionDigits = 0
        nf.maximumFractionDigits = 0
        return nf.format(amount)
    }
}
