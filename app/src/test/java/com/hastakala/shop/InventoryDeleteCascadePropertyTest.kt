package com.hastakala.shop

import com.hastakala.shop.model.Product
import com.hastakala.shop.model.ProductVariant
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import java.util.UUID

// ---------------------------------------------------------------------------
// In-memory store that mirrors the Room CASCADE delete behaviour
// ---------------------------------------------------------------------------

class InMemoryProductStore {
    private val products = mutableMapOf<String, Product>()
    private val variants = mutableMapOf<String, ProductVariant>()

    fun insertProduct(product: Product) {
        products[product.id] = product
    }

    fun insertVariant(variant: ProductVariant) {
        variants[variant.id] = variant
    }

    /** Deletes the product and all variants whose productId matches — mirrors CASCADE. */
    fun deleteProduct(productId: String) {
        products.remove(productId)
        variants.values.removeAll { it.productId == productId }
    }

    fun getVariantsByProductId(productId: String): List<ProductVariant> =
        variants.values.filter { it.productId == productId }
}

// ---------------------------------------------------------------------------
// Arbitraries
// ---------------------------------------------------------------------------

private val arbProductUuid: Arb<String> = Arb.uuid().map { it.toString() }

private val arbProductGen: Arb<Product> = arbitrary {
    Product(
        id = arbProductUuid.bind(),
        userId = "test-user",
        name = Arb.string(1..30, Arb.alphanumeric()).bind()
    )
}

private fun arbVariantsFor(productId: String, count: Int): List<ProductVariant> =
    (1..count).map {
        ProductVariant(
            id = UUID.randomUUID().toString(),
            userId = "test-user",
            productId = productId,
            colorOrDesign = "Color$it",
            stock = (1..100).random(),
            unitPrice = (1..500).random().toDouble()
        )
    }

// ---------------------------------------------------------------------------
// Property-Based Test
// ---------------------------------------------------------------------------

class InventoryDeleteCascadePropertyTest : StringSpec({

    // Feature: hasta-kala-shop, Property: delete removes all variants
    "deleting a product removes all its variants from the store" {
        // Validates: Requirements 3.3
        checkAll(100, arbProductGen, Arb.int(1..10)) { product, variantCount ->
            val store = InMemoryProductStore()
            store.insertProduct(product)
            arbVariantsFor(product.id, variantCount).forEach { store.insertVariant(it) }

            store.deleteProduct(product.id)

            store.getVariantsByProductId(product.id).shouldBeEmpty()
        }
    }
})
