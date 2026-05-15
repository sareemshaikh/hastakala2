package com.hastakala.testshop.model

/**
 * Convenience wrapper that pairs a [Product] with all its [ProductVariant]s.
 * Used by InventoryViewModel to expose a combined list to the UI.
 */
data class ProductWithVariants(
    val product: Product,
    val variants: List<ProductVariant>
)
