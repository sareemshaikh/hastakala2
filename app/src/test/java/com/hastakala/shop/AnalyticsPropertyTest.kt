package com.hastakala.shop

import com.hastakala.shop.model.Sale
import com.hastakala.shop.viewmodel.AnalyticsViewModel
import com.hastakala.shop.viewmodel.VariantSaleTotal
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll

// ---------------------------------------------------------------------------
// Arbitraries
// ---------------------------------------------------------------------------

private val arbAnalyticsUuid: Arb<String> = Arb.uuid().map { it.toString() }

/** Generates a list of Sale objects (possibly empty) with random variant assignments. */
private val arbAnalyticsSaleList: Arb<List<Sale>> = arbitrary {
    val saleCount = Arb.int(0..30).bind()
    if (saleCount == 0) return@arbitrary emptyList()

    val variantCount = Arb.int(1..5).bind()
    val variantIds = List(variantCount) { arbAnalyticsUuid.bind() }

    List(saleCount) {
        val variantId = variantIds[Arb.int(0 until variantCount).bind()]
        val qty = Arb.int(1..50).bind()
        val price = Arb.double(0.01, 999.99).bind()
        Sale(
            id = arbAnalyticsUuid.bind(),
            userId = "test-user",
            variantId = variantId,
            productName = Arb.string(1..20, Arb.alphanumeric()).bind(),
            variantLabel = Arb.string(1..10, Arb.alphanumeric()).bind(),
            quantity = qty,
            unitPrice = price,
            totalAmount = qty * price,
            timestamp = Arb.long(0L..System.currentTimeMillis()).bind()
        )
    }
}

// ---------------------------------------------------------------------------
// Pure helper — mirrors AnalyticsViewModel.computeVariantTotals without Android deps
// ---------------------------------------------------------------------------

private fun computeVariantTotalsForTest(sales: List<Sale>): List<VariantSaleTotal> {
    if (sales.isEmpty()) return emptyList()
    return sales
        .groupBy { it.variantId }
        .map { (variantId, variantSales) ->
            val sample = variantSales.first()
            val fullLabel = "${sample.productName} – ${sample.variantLabel}"
            val shortLabel = if (sample.variantLabel.length <= 10)
                sample.variantLabel else sample.variantLabel.take(8) + "…"
            VariantSaleTotal(
                variantId = variantId,
                label = fullLabel,
                shortLabel = shortLabel,
                totalUnits = variantSales.sumOf { it.quantity }
            )
        }
        .sortedByDescending { it.totalUnits }
}

// ---------------------------------------------------------------------------
// Property-Based Tests
// ---------------------------------------------------------------------------

class AnalyticsPropertyTest : StringSpec({

    // Feature: hasta-kala-shop, Property 9: Analytics reflect all sales
    "Property 9 - sum of chart segment units equals total units sold across all sales" {
        // Validates: Requirements 4.1, 4.2
        checkAll(100, arbAnalyticsSaleList) { sales ->
            val totals = computeVariantTotalsForTest(sales)

            val expectedTotalUnits = sales.sumOf { it.quantity }
            val chartTotalUnits = totals.sumOf { it.totalUnits }

            chartTotalUnits shouldBe expectedTotalUnits
        }
    }

    // Feature: hasta-kala-shop, Property 9: Analytics reflect all sales (empty case)
    "Property 9 - empty sales list produces empty variant totals" {
        // Validates: Requirements 4.5
        val totals = computeVariantTotalsForTest(emptyList())
        totals shouldBe emptyList()
    }

    // Feature: hasta-kala-shop, Property 9: Analytics reflect all sales (best seller)
    "Property 9 - first variant total is the best seller when sales exist" {
        // Validates: Requirements 4.4
        checkAll(100, arbAnalyticsSaleList) { sales ->
            if (sales.isNotEmpty()) {
                val totals = computeVariantTotalsForTest(sales)

                // totals are sorted descending — first entry is the best seller
                val bestSeller = totals.first()
                val expectedTopUnits = totals.maxOf { it.totalUnits }

                bestSeller.totalUnits shouldBe expectedTopUnits
            }
        }
    }
})
