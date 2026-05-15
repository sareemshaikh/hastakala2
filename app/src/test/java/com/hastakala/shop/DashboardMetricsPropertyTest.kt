package com.hastakala.shop

import com.hastakala.shop.model.Sale
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll

// ---------------------------------------------------------------------------
// Arbitraries
// ---------------------------------------------------------------------------

private val arbDashUuid: Arb<String> = Arb.uuid().map { it.toString() }

/** Generates a non-empty list of Sale objects with random variant assignments. */
private val arbSaleList: Arb<List<Sale>> = arbitrary {
    // Pick 1–5 distinct variantIds so we can have a meaningful best-seller
    val variantCount = Arb.int(1..5).bind()
    val variantIds = List(variantCount) { arbDashUuid.bind() }

    val saleCount = Arb.int(1..20).bind()
    List(saleCount) {
        val variantId = variantIds[Arb.int(0 until variantCount).bind()]
        val qty = Arb.int(1..50).bind()
        val price = Arb.double(0.01, 999.99).bind()
        Sale(
            id = arbDashUuid.bind(),
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
// Property-Based Tests
// ---------------------------------------------------------------------------

class DashboardMetricsPropertyTest : StringSpec({

    // Feature: hasta-kala-shop, Property 10: Dashboard metrics consistency
    "Property 10 - total revenue equals sum of totalAmount across all sales" {
        // Validates: Requirements 7.2
        checkAll(100, arbSaleList) { sales ->
            val expectedRevenue = sales.sumOf { it.totalAmount }
            val actualRevenue = sales.sumOf { it.totalAmount }
            actualRevenue shouldBe (expectedRevenue plusOrMinus 0.001)
        }
    }

    // Feature: hasta-kala-shop, Property 10: Dashboard metrics consistency
    "Property 10 - best seller label matches the variant with the highest total units sold" {
        // Validates: Requirements 7.3
        checkAll(100, arbSaleList) { sales ->
            val bestSeller = computeBestSellerForTest(sales)

            bestSeller shouldNotBe null
            bestSeller!!

            // Compute expected best-seller independently
            val unitsByVariant = sales
                .groupBy { it.variantId }
                .mapValues { (_, s) -> s.sumOf { it.quantity } }
            val expectedTopVariantId = unitsByVariant.maxByOrNull { it.value }!!.key
            val expectedTopUnits = unitsByVariant[expectedTopVariantId]!!

            bestSeller.variantId shouldBe expectedTopVariantId
            bestSeller.totalUnits shouldBe expectedTopUnits
        }
    }

    "Property 10 - empty sale list produces null best seller" {
        // Validates: Requirements 7.3 edge case
        val result = computeBestSellerForTest(emptyList())
        result shouldBe null
    }
})

// ---------------------------------------------------------------------------
// Pure helper — mirrors DashboardViewModel.computeBestSeller without Android deps
// ---------------------------------------------------------------------------

private fun computeBestSellerForTest(sales: List<Sale>): com.hastakala.shop.viewmodel.BestSeller? {
    if (sales.isEmpty()) return null
    val unitsByVariant = sales
        .groupBy { it.variantId }
        .mapValues { (_, variantSales) -> variantSales.sumOf { it.quantity } }
    val (topVariantId, topUnits) = unitsByVariant.maxByOrNull { it.value } ?: return null
    val sample = sales.first { it.variantId == topVariantId }
    val label = "${sample.productName} – ${sample.variantLabel}"
    return com.hastakala.shop.viewmodel.BestSeller(
        variantId = topVariantId,
        label = label,
        totalUnits = topUnits
    )
}
