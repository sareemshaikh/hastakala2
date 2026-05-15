package com.hastakala.testshop.repository

import com.hastakala.testshop.model.Product
import com.hastakala.testshop.model.ProductVariant
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getAllProducts(): Flow<List<Product>>
    fun getAllVariants(): Flow<List<ProductVariant>>
    fun getAllVariantsForProduct(productId: String): Flow<List<ProductVariant>>
    suspend fun insertProduct(product: Product)
    suspend fun insertVariant(variant: ProductVariant)
    suspend fun updateStock(variantId: String, newQty: Int)
    suspend fun deleteProduct(productId: String)
}
