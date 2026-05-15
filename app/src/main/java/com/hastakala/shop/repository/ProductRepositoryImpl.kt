package com.hastakala.testshop.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hastakala.testshop.data.local.ProductDao
import com.hastakala.testshop.model.Product
import com.hastakala.testshop.model.ProductVariant
import com.hastakala.testshop.util.AlertHelper
import com.hastakala.testshop.util.toMap
import com.hastakala.testshop.worker.FirestoreSyncWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.tasks.await

class ProductRepositoryImpl(
    private val productDao: ProductDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val context: Context
) : ProductRepository {

    private val syncRepository = SyncRepository(context)

    /** Returns the current user's UID, or null if not authenticated. */
    private fun uid(): String? = auth.currentUser?.uid

    // -------------------------------------------------------------------------
    // Reads — scoped to the authenticated user
    // -------------------------------------------------------------------------

    override fun getAllProducts(): Flow<List<Product>> {
        val userId = uid() ?: return emptyFlow()
        return productDao.getAllProducts(userId)
    }

    override fun getAllVariants(): Flow<List<ProductVariant>> {
        val userId = uid() ?: return emptyFlow()
        return productDao.getAllVariants(userId)
    }

    override fun getAllVariantsForProduct(productId: String): Flow<List<ProductVariant>> {
        val userId = uid() ?: return emptyFlow()
        return productDao.getVariantsByProduct(productId, userId)
    }

    // -------------------------------------------------------------------------
    // Writes — always stamp userId on the entity before persisting
    // -------------------------------------------------------------------------

    override suspend fun insertProduct(product: Product) {
        val userId = uid() ?: return
        val stamped = product.copy(userId = userId)
        productDao.insertProduct(stamped)
        syncToFirestore(FirestoreSyncWorker.TYPE_PRODUCT, stamped.id) {
            firestore.collection("users").document(userId)
                .collection("products").document(stamped.id)
                .set(stamped.toMap()).await()
        }
    }

    override suspend fun insertVariant(variant: ProductVariant) {
        val userId = uid() ?: return
        val stamped = variant.copy(userId = userId)
        productDao.insertVariant(stamped)
        syncToFirestore(FirestoreSyncWorker.TYPE_VARIANT, stamped.id) {
            firestore.collection("users").document(userId)
                .collection("product_variants").document(stamped.id)
                .set(stamped.toMap()).await()
        }
    }

    override suspend fun updateStock(variantId: String, newQty: Int) {
        val userId = uid() ?: return
        productDao.updateStock(variantId, newQty)
        val variant = productDao.getVariantById(variantId) ?: return
        // Clear low-stock alert if restocked above threshold (Req 5.3)
        AlertHelper.clearAlertsForRestockedVariants(context, listOf(variant))
        syncToFirestore(FirestoreSyncWorker.TYPE_VARIANT, variantId) {
            firestore.collection("users").document(userId)
                .collection("product_variants").document(variantId)
                .set(variant.toMap()).await()
        }
    }

    override suspend fun deleteProduct(productId: String) {
        val userId = uid() ?: return
        val product = productDao.getProductById(productId, userId) ?: return
        productDao.deleteProduct(product)
        syncToFirestore(FirestoreSyncWorker.TYPE_DELETE_PRODUCT, productId) {
            firestore.collection("users").document(userId)
                .collection("products").document(productId)
                .delete().await()
        }
    }

    // -------------------------------------------------------------------------
    // Helper: try Firestore immediately, queue on failure
    // -------------------------------------------------------------------------

    private suspend fun syncToFirestore(type: String, entityId: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            syncRepository.enqueue(type, entityId)
        }
    }
}
