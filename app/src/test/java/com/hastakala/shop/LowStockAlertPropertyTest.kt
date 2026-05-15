package com.hastakala.shop

import com.hastakala.shop.model.ProductVariant
import com.hastakala.shop.util.AlertHelper
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import java.util.UUID

// ---------------------------------------------------------------------------
// Arbitraries
// ---------------------------------------------------------------------------

private val arbVariantId: Arb<String> = Arb.uuid().map { it.toString() }

/** Variant whose stock is at or below the threshold (low stock). */
private fun arbLowStockVariant(threshold: Int = AlertHelper.LOW_STOCK_THRESHOLD): Arb<ProductVariant> =
    arbitrary {
        ProductVariant(
            id = arbVariantId.bind(),
            userId = "test-user",
            productId = arbVariantId.bind(),
            colorOrDesign = Arb.string(1..20, Arb.alphanumeric()).bind(),
            stock = Arb.int(0..threshold).bind(),
            unitPrice = Arb.double(0.01, 999.99).bind()
        )
    }

/** Variant whose stock is strictly above the threshold (healthy stock). */
private fun arbHealthyVariant(threshold: Int = AlertHelper.LOW_STOCK_THRESHOLD): Arb<ProductVariant> =
    arbitrary {
        ProductVariant(
            id = arbVariantId.bind(),
            userId = "test-user",
            productId = arbVariantId.bind(),
            colorOrDesign = Arb.string(1..20, Arb.alphanumeric()).bind(),
            stock = Arb.int(threshold + 1..500).bind(),
            unitPrice = Arb.double(0.01, 999.99).bind()
        )
    }

/** A list of variants mixing low-stock and healthy variants. */
private fun arbMixedVariantList(threshold: Int = AlertHelper.LOW_STOCK_THRESHOLD): Arb<List<ProductVariant>> =
    arbitrary {
        val lowCount = Arb.int(1..5).bind()
        val healthyCount = Arb.int(0..5).bind()
        val lowVariants = List(lowCount) { arbLowStockVariant(threshold).bind() }
        val healthyVariants = List(healthyCount) { arbHealthyVariant(threshold).bind() }
        (lowVariants + healthyVariants).shuffled()
    }

// ---------------------------------------------------------------------------
// Property-Based Tests
// ---------------------------------------------------------------------------

class LowStockAlertPropertyTest : StringSpec({

    // Feature: hasta-kala-shop, Property 3: Low stock alert fires at threshold
    "Property 3 - checkLowStock includes every variant at or below the threshold" {
        // Validates: Requirements 5.1, 5.2
        checkAll(100, arbMixedVariantList()) { variants ->
            val threshold = AlertHelper.LOW_STOCK_THRESHOLD
            val lowStockResult = AlertHelper.checkLowStock(variants, threshold)

            // Every variant at or below threshold must appear in the result
            variants.filter { it.stock <= threshold }.forEach { variant ->
                lowStockResult shouldContain variant
            }

            // Every variant above threshold must NOT appear in the result
            variants.filter { it.stock > threshold }.forEach { variant ->
                lowStockResult shouldNotContain variant
            }
        }
    }

    // Feature: hasta-kala-shop, Property 4: Restock clears alert
    "Property 4 - restocking a variant above the threshold removes it from the low-stock list" {
        // Validates: Requirements 5.3
        checkAll(100, arbLowStockVariant()) { lowVariant ->
            val threshold = AlertHelper.LOW_STOCK_THRESHOLD

            // Confirm it starts in the low-stock list
            val before = AlertHelper.checkLowStock(listOf(lowVariant), threshold)
            before shouldContain lowVariant

            // Restock to strictly above threshold
            val restockedVariant = lowVariant.copy(stock = threshold + 1)
            val after = AlertHelper.checkLowStock(listOf(restockedVariant), threshold)
            after shouldNotContain restockedVariant
        }
    }
})
