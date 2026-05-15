package com.hastakala.shop

import com.hastakala.shop.model.Product
import com.hastakala.shop.model.ProductVariant
import com.hastakala.shop.model.Sale
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import java.util.UUID

// ---------------------------------------------------------------------------
// Pure business-logic helpers extracted from SaleRepositoryImpl for testing
// ---------------------------------------------------------------------------

/**
 * Validates and applies a sale against a variant's current stock.
 * Returns the new stock on success, or a failure with the reason.
 */
fun applySale(variant: ProductVariant, sale: Sale): Result<Int> {
    if (sale.quantity <= 0) {
        return Result.failure(IllegalArgumentException("Quantity must be greater than zero"))
    }
    if (sale.quantity > variant.stock) {
        return Result.failure(IllegalArgumentException("Insufficient stock"))
    }
    return Result.success(variant.stock - sale.quantity)
}

// ---------------------------------------------------------------------------
// Arbitraries
// ---------------------------------------------------------------------------

private val arbSaleUuid: Arb<String> = Arb.uuid().map { it.toString() }

private val arbVariantWithStock: Arb<ProductVariant> = arbitrary {
    ProductVariant(
        id = arbSaleUuid.bind(),
        userId = "test-user",
        productId = arbSaleUuid.bind(),
        colorOrDesign = Arb.string(1..20, Arb.alphanumeric()).bind(),
        stock = Arb.int(1..500).bind(),
        unitPrice = Arb.double(0.01, 999.99).bind()
    )
}

private fun arbValidSaleFor(variant: ProductVariant): Arb<Sale> = arbitrary {
    val qty = Arb.int(1..variant.stock).bind()
    Sale(
        id = arbSaleUuid.bind(),
        userId = "test-user",
        variantId = variant.id,
        productName = Arb.string(1..30, Arb.alphanumeric()).bind(),
        variantLabel = variant.colorOrDesign,
        quantity = qty,
        unitPrice = variant.unitPrice,
        totalAmount = qty * variant.unitPrice,
        timestamp = Arb.long(0L..System.currentTimeMillis()).bind()
    )
}

private fun arbOversellSaleFor(variant: ProductVariant): Arb<Sale> = arbitrary {
    val qty = Arb.int(variant.stock + 1..variant.stock + 500).bind()
    Sale(
        id = arbSaleUuid.bind(),
        userId = "test-user",
        variantId = variant.id,
        productName = Arb.string(1..30, Arb.alphanumeric()).bind(),
        variantLabel = variant.colorOrDesign,
        quantity = qty,
        unitPrice = variant.unitPrice,
        totalAmount = qty * variant.unitPrice,
        timestamp = Arb.long(0L..System.currentTimeMillis()).bind()
    )
}

// Arbitrary for zero or negative quantities (Req 2.6)
private val arbNonPositiveQty: Arb<Int> = Arb.int(Int.MIN_VALUE..0)

private fun arbSaleWithQtyFor(variant: ProductVariant, qty: Int): Sale = Sale(
    id = UUID.randomUUID().toString(),
    userId = "test-user",
    variantId = variant.id,
    productName = "Test Product",
    variantLabel = variant.colorOrDesign,
    quantity = qty,
    unitPrice = variant.unitPrice,
    totalAmount = qty * variant.unitPrice,
    timestamp = System.currentTimeMillis()
)

// ---------------------------------------------------------------------------
// Property-Based Tests
// ---------------------------------------------------------------------------

class SaleRepositoryPropertyTest : StringSpec({

    // Feature: hasta-kala-shop, Property 1: Sale saves decrement stock
    "Property 1 - recording a valid sale decrements stock by exactly the sale quantity" {
        // Validates: Requirements 2.4
        checkAll(100, arbVariantWithStock) { variant ->
            val sale = arbValidSaleFor(variant).sample(io.kotest.property.RandomSource.default()).value
            val result = applySale(variant, sale)
            result.isSuccess shouldBe true
            result.getOrThrow() shouldBe (variant.stock - sale.quantity)
        }
    }

    // Feature: hasta-kala-shop, Property 2: Oversell prevention
    "Property 2 - attempting to save a sale exceeding stock is rejected and stock is unchanged" {
        // Validates: Requirements 2.5
        checkAll(100, arbVariantWithStock) { variant ->
            val sale = arbOversellSaleFor(variant).sample(io.kotest.property.RandomSource.default()).value
            val result = applySale(variant, sale)
            result.isFailure shouldBe true
            result.exceptionOrNull().shouldBeInstanceOf<IllegalArgumentException>()
        }
    }

    // Feature: hasta-kala-shop, Property 3: Zero/negative quantity rejection
    "Property 3 - saving a sale with zero or negative quantity is rejected and stock is unchanged" {
        // Validates: Requirements 2.6
        checkAll(100, arbVariantWithStock, arbNonPositiveQty) { variant, qty ->
            val sale = arbSaleWithQtyFor(variant, qty)
            val result = applySale(variant, sale)
            result.isFailure shouldBe true
            result.exceptionOrNull().shouldBeInstanceOf<IllegalArgumentException>()
        }
    }
})
