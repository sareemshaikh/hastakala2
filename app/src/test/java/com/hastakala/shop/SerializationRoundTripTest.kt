package com.hastakala.shop

import com.hastakala.shop.model.Product
import com.hastakala.shop.model.ProductVariant
import com.hastakala.shop.model.Sale
import com.hastakala.shop.util.toMap
import com.hastakala.shop.util.toProduct
import com.hastakala.shop.util.toProductVariant
import com.hastakala.shop.util.toSale
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll

// ---------------------------------------------------------------------------
// Arbitraries
// ---------------------------------------------------------------------------

val arbUuid: Arb<String> = Arb.uuid().map { it.toString() }

val arbSale: Arb<Sale> = arbitrary {
    val qty = Arb.int(1..100).bind()
    val price = Arb.double(0.01, 9999.99).bind()
    Sale(
        id = arbUuid.bind(),
        userId = arbUuid.bind(),
        variantId = arbUuid.bind(),
        productName = Arb.string(1..50, Arb.alphanumeric()).bind(),
        variantLabel = Arb.string(1..30, Arb.alphanumeric()).bind(),
        quantity = qty,
        unitPrice = price,
        totalAmount = qty * price,
        timestamp = Arb.long(0L..System.currentTimeMillis()).bind()
    )
}

val arbProduct: Arb<Product> = arbitrary {
    Product(
        id = arbUuid.bind(),
        userId = arbUuid.bind(),
        name = Arb.string(1..50, Arb.alphanumeric()).bind(),
        imageResId = Arb.int(1..Int.MAX_VALUE).orNull().bind()
    )
}

val arbProductVariant: Arb<ProductVariant> = arbitrary {
    ProductVariant(
        id = arbUuid.bind(),
        userId = arbUuid.bind(),
        productId = arbUuid.bind(),
        colorOrDesign = Arb.string(1..30, Arb.alphanumeric()).bind(),
        stock = Arb.int(0..1000).bind(),
        unitPrice = Arb.double(0.01, 9999.99).bind()
    )
}

// ---------------------------------------------------------------------------
// Property-Based Tests
// ---------------------------------------------------------------------------

class SerializationRoundTripTest : StringSpec({

    // Feature: hasta-kala-shop, Property 7: Sale entity round-trip serialization
    "Property 7 - Sale toMap then toSale produces equal object" {
        // Validates: Requirements 8.4
        checkAll(100, arbSale) { sale ->
            sale.toMap().toSale() shouldBe sale
        }
    }

    // Feature: hasta-kala-shop, Property 8: Product/Variant entity round-trip serialization
    "Property 8a - Product toMap then toProduct produces equal object" {
        // Validates: Requirements 8.4
        checkAll(100, arbProduct) { product ->
            product.toMap().toProduct() shouldBe product
        }
    }

    // Feature: hasta-kala-shop, Property 8: Product/Variant entity round-trip serialization
    "Property 8b - ProductVariant toMap then toProductVariant produces equal object" {
        // Validates: Requirements 8.4
        checkAll(100, arbProductVariant) { variant ->
            variant.toMap().toProductVariant() shouldBe variant
        }
    }
})
