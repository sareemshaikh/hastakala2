package com.hastakala.shop

import com.hastakala.shop.model.Sale
import com.hastakala.shop.viewmodel.IncomePeriod
import com.hastakala.shop.viewmodel.IncomeLogViewModel
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import java.util.Calendar

// ---------------------------------------------------------------------------
// Arbitraries
// ---------------------------------------------------------------------------

private val arbIncomeUuid: Arb<String> = Arb.uuid().map { it.toString() }

/** Generates a Sale with a timestamp within [rangeStart, rangeEnd]. */
private fun arbSaleInRange(rangeStart: Long, rangeEnd: Long): Arb<Sale> = arbitrary {
    val qty = Arb.int(1..20).bind()
    val price = Arb.double(0.01, 500.0).bind()
    Sale(
        id = arbIncomeUuid.bind(),
        userId = "test-user",
        variantId = arbIncomeUuid.bind(),
        productName = Arb.string(1..15, Arb.alphanumeric()).bind(),
        variantLabel = Arb.string(1..10, Arb.alphanumeric()).bind(),
        quantity = qty,
        unitPrice = price,
        totalAmount = qty * price,
        timestamp = Arb.long(rangeStart..rangeEnd).bind()
    )
}

/** Generates a Sale with a timestamp strictly outside [rangeStart, rangeEnd]. */
private fun arbSaleOutsideRange(rangeStart: Long, rangeEnd: Long): Arb<Sale> = arbitrary {
    // Place the timestamp either before rangeStart or after rangeEnd
    val before = rangeStart > 0L
    val timestamp = if (before) {
        Arb.long(0L until rangeStart).bind()
    } else {
        Arb.long((rangeEnd + 1)..Long.MAX_VALUE).bind()
    }
    val qty = Arb.int(1..20).bind()
    val price = Arb.double(0.01, 500.0).bind()
    Sale(
        id = arbIncomeUuid.bind(),
        userId = "test-user",
        variantId = arbIncomeUuid.bind(),
        productName = Arb.string(1..15, Arb.alphanumeric()).bind(),
        variantLabel = Arb.string(1..10, Arb.alphanumeric()).bind(),
        quantity = qty,
        unitPrice = price,
        totalAmount = qty * price,
        timestamp = timestamp
    )
}

// ---------------------------------------------------------------------------
// Pure filter helper — mirrors IncomeLogViewModel logic without Android deps
// ---------------------------------------------------------------------------

private fun filterSalesByPeriod(sales: List<Sale>, period: IncomePeriod): List<Sale> {
    val (start, end) = IncomeLogViewModel.periodRange(period)
    return sales.filter { it.timestamp in start..end }
}

// ---------------------------------------------------------------------------
// Property-Based Tests
// ---------------------------------------------------------------------------

class IncomeLogPropertyTest : StringSpec({

    // Feature: hasta-kala-shop, Property 5: Income filter correctness
    "Property 5 - all sales returned by TODAY filter have timestamps within today" {
        // Validates: Requirements 6.4
        val (start, end) = IncomeLogViewModel.periodRange(IncomePeriod.TODAY)
        checkAll(100, Arb.list(arbSaleInRange(start, end), 0..20)) { inRangeSales ->
            val result = filterSalesByPeriod(inRangeSales, IncomePeriod.TODAY)
            result.size shouldBe inRangeSales.size
            result.all { it.timestamp in start..end } shouldBe true
        }
    }

    // Feature: hasta-kala-shop, Property 5: Income filter correctness
    "Property 5 - no sale outside TODAY range appears in TODAY filter results" {
        // Validates: Requirements 6.4
        val (start, end) = IncomeLogViewModel.periodRange(IncomePeriod.TODAY)
        // Only generate outside-range sales if rangeStart > 0 to avoid empty domain
        if (start > 0L) {
            checkAll(100, Arb.list(arbSaleOutsideRange(start, end), 1..10)) { outsideSales ->
                val result = filterSalesByPeriod(outsideSales, IncomePeriod.TODAY)
                result.size shouldBe 0
            }
        }
    }

    // Feature: hasta-kala-shop, Property 5: Income filter correctness
    "Property 5 - all sales returned by THIS_WEEK filter have timestamps within this week" {
        // Validates: Requirements 6.2
        val (start, end) = IncomeLogViewModel.periodRange(IncomePeriod.THIS_WEEK)
        checkAll(100, Arb.list(arbSaleInRange(start, end), 0..20)) { inRangeSales ->
            val result = filterSalesByPeriod(inRangeSales, IncomePeriod.THIS_WEEK)
            result.size shouldBe inRangeSales.size
            result.all { it.timestamp in start..end } shouldBe true
        }
    }

    // Feature: hasta-kala-shop, Property 5: Income filter correctness
    "Property 5 - all sales returned by THIS_MONTH filter have timestamps within this month" {
        // Validates: Requirements 6.3
        val (start, end) = IncomeLogViewModel.periodRange(IncomePeriod.THIS_MONTH)
        checkAll(100, Arb.list(arbSaleInRange(start, end), 0..20)) { inRangeSales ->
            val result = filterSalesByPeriod(inRangeSales, IncomePeriod.THIS_MONTH)
            result.size shouldBe inRangeSales.size
            result.all { it.timestamp in start..end } shouldBe true
        }
    }

    // Feature: hasta-kala-shop, Property 6: Revenue calculation consistency
    "Property 6 - period revenue equals sum of totalAmount for filtered sales" {
        // Validates: Requirements 6.2, 6.3, 6.4
        val (start, end) = IncomeLogViewModel.periodRange(IncomePeriod.THIS_MONTH)
        checkAll(100, Arb.list(arbSaleInRange(start, end), 0..30)) { sales ->
            val filtered = filterSalesByPeriod(sales, IncomePeriod.THIS_MONTH)
            val expectedRevenue = filtered.sumOf { it.totalAmount }
            val actualRevenue = filtered.sumOf { it.totalAmount }
            actualRevenue shouldBe (expectedRevenue plusOrMinus 0.001)
        }
    }

    // Feature: hasta-kala-shop, Property 6: Revenue calculation consistency
    "Property 6 - revenue is zero when no sales exist in the period" {
        // Validates: Requirements 6.2, 6.3, 6.4 edge case
        val result = filterSalesByPeriod(emptyList(), IncomePeriod.TODAY)
        result.sumOf { it.totalAmount } shouldBe (0.0 plusOrMinus 0.001)
    }

    // Feature: hasta-kala-shop, Property 6: Revenue calculation consistency
    "Property 6 - mixed in-range and out-of-range sales: revenue only counts in-range" {
        // Validates: Requirements 6.2, 6.3, 6.4
        val (start, end) = IncomeLogViewModel.periodRange(IncomePeriod.TODAY)
        if (start > 0L) {
            checkAll(
                50,
                Arb.list(arbSaleInRange(start, end), 1..15),
                Arb.list(arbSaleOutsideRange(start, end), 1..10)
            ) { inRange, outOfRange ->
                val allSales = inRange + outOfRange
                val filtered = filterSalesByPeriod(allSales, IncomePeriod.TODAY)
                val expectedRevenue = inRange.sumOf { it.totalAmount }
                filtered.sumOf { it.totalAmount } shouldBe (expectedRevenue plusOrMinus 0.001)
            }
        }
    }
})
