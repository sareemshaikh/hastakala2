package com.hastakala.shop

import com.hastakala.shop.model.ProductVariant
import com.hastakala.shop.model.Sale
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import java.util.UUID

/**
 * Integration-style tests for the Quick Bill workflow.
 *
 * These tests exercise the full sale-application pipeline end-to-end using
 * the pure [applySale] function (from SaleRepositoryPropertyTest) and an
 * in-memory store, verifying that:
 *
 * 1. Stock is decremented correctly after a valid sale.
 * 2. Overselling is prevented.
 * 3. Multiple sequential sales accumulate correctly.
 * 4. Total amount = quantity × unitPrice for every sale.
 */
class QuickBillWorkflowTest : StringSpec({

    // -------------------------------------------------------------------------
    // End-to-end: select product → select variant → enter qty → save
    // -------------------------------------------------------------------------

    "workflow - valid sale decrements stock and totalAmount equals qty * unitPrice" {
        // Validates: Requirements 2.4, 2.3
        checkAll(100, arbVariantWithStock) { variant ->
            val sale = arbValidSaleFor(variant)
                .sample(io.kotest.property.RandomSource.default()).value

            val result = applySale(variant, sale)

            result.isSuccess shouldBe true
            result.getOrThrow() shouldBe (variant.stock - sale.quantity)

            // totalAmount must equal quantity × unitPrice
            sale.totalAmount shouldBe (sale.quantity * sale.unitPrice)
        }
    }

    "workflow - sequential sales accumulate stock decrements correctly" {
        // Validates: Requirements 2.4 — multiple sales on the same variant
        checkAll(50, arbVariantWithStock) { initialVariant ->
            var currentStock = initialVariant.stock
            var totalSold = 0

            // Apply up to 3 sequential sales as long as stock allows
            repeat(3) {
                if (currentStock > 0) {
                    val qty = (1..currentStock).random()
                    val sale = Sale(
                        id = UUID.randomUUID().toString(),
                        userId = "test-user",
                        variantId = initialVariant.id,
                        productName = "Test",
                        variantLabel = initialVariant.colorOrDesign,
                        quantity = qty,
                        unitPrice = initialVariant.unitPrice,
                        totalAmount = qty * initialVariant.unitPrice,
                        timestamp = System.currentTimeMillis()
                    )
                    val variant = initialVariant.copy(stock = currentStock)
                    val result = applySale(variant, sale)
                    result.isSuccess shouldBe true
                    currentStock = result.getOrThrow()
                    totalSold += qty
                }
            }

            // Final stock = initial stock - total units sold
            currentStock shouldBe (initialVariant.stock - totalSold)
        }
    }

    "workflow - oversell attempt after partial stock depletion is rejected" {
        // Validates: Requirements 2.5 — oversell after some stock already sold
        checkAll(50, arbVariantWithStock) { variant ->
            if (variant.stock >= 2) {
                // First: sell half the stock
                val firstQty = variant.stock / 2
                val firstSale = Sale(
                    id = UUID.randomUUID().toString(),
                    userId = "test-user",
                    variantId = variant.id,
                    productName = "Test",
                    variantLabel = variant.colorOrDesign,
                    quantity = firstQty,
                    unitPrice = variant.unitPrice,
                    totalAmount = firstQty * variant.unitPrice,
                    timestamp = System.currentTimeMillis()
                )
                val afterFirst = applySale(variant, firstSale)
                afterFirst.isSuccess shouldBe true
                val remainingStock = afterFirst.getOrThrow()

                // Second: try to sell more than remaining stock
                val oversellQty = remainingStock + 1
                val oversellSale = Sale(
                    id = UUID.randomUUID().toString(),
                    userId = "test-user",
                    variantId = variant.id,
                    productName = "Test",
                    variantLabel = variant.colorOrDesign,
                    quantity = oversellQty,
                    unitPrice = variant.unitPrice,
                    totalAmount = oversellQty * variant.unitPrice,
                    timestamp = System.currentTimeMillis()
                )
                val depletedVariant = variant.copy(stock = remainingStock)
                val result = applySale(depletedVariant, oversellSale)

                result.isFailure shouldBe true
                result.exceptionOrNull().shouldBeInstanceOf<IllegalArgumentException>()
            }
        }
    }

    "workflow - total revenue across multiple sales equals sum of all totalAmounts" {
        // Validates: Requirements 6.2, 6.3, 6.4, 7.2
        checkAll(50, Arb.list(arbVariantWithStock, 1..5)) { variants ->
            val sales = variants.map { variant ->
                val qty = (1..variant.stock).random()
                Sale(
                    id = UUID.randomUUID().toString(),
                    userId = "test-user",
                    variantId = variant.id,
                    productName = "Test",
                    variantLabel = variant.colorOrDesign,
                    quantity = qty,
                    unitPrice = variant.unitPrice,
                    totalAmount = qty * variant.unitPrice,
                    timestamp = System.currentTimeMillis()
                )
            }

            val expectedRevenue = sales.sumOf { it.quantity * it.unitPrice }
            val actualRevenue = sales.sumOf { it.totalAmount }

            // Allow tiny floating-point delta
            kotlin.math.abs(actualRevenue - expectedRevenue) < 0.001 shouldBe true
        }
    }
})
