package com.hastakala.testshop.data.local

import androidx.room.*
import com.hastakala.testshop.model.Product
import com.hastakala.testshop.model.ProductVariant
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    // --- Product operations ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    /** Returns only products belonging to the given user. */
    @Query("SELECT * FROM products WHERE userId = :userId")
    fun getAllProducts(userId: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :productId AND userId = :userId")
    suspend fun getProductById(productId: String, userId: String): Product?

    // --- ProductVariant operations ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariant(variant: ProductVariant)

    @Update
    suspend fun updateVariant(variant: ProductVariant)

    @Delete
    suspend fun deleteVariant(variant: ProductVariant)

    @Query("SELECT * FROM product_variants WHERE productId = :productId AND userId = :userId")
    fun getVariantsByProduct(productId: String, userId: String): Flow<List<ProductVariant>>

    @Query("SELECT * FROM product_variants WHERE id = :variantId AND userId = :userId")
    suspend fun getVariantById(variantId: String, userId: String): ProductVariant?

    /** Used internally by SaleRepositoryImpl — no userId filter needed since variantId is globally unique. */
    @Query("SELECT * FROM product_variants WHERE id = :variantId")
    suspend fun getVariantById(variantId: String): ProductVariant?

    @Query("UPDATE product_variants SET stock = :newQty WHERE id = :variantId")
    suspend fun updateStock(variantId: String, newQty: Int)

    /** Returns only variants belonging to the given user. */
    @Query("SELECT * FROM product_variants WHERE userId = :userId")
    fun getAllVariants(userId: String): Flow<List<ProductVariant>>
}
