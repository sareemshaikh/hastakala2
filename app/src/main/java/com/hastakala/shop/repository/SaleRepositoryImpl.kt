package com.hastakala.testshop.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hastakala.testshop.data.local.ProductDao
import com.hastakala.testshop.data.local.SaleDao
import com.hastakala.testshop.model.Sale
import com.hastakala.testshop.util.AlertHelper
import com.hastakala.testshop.util.toMap
import com.hastakala.testshop.worker.FirestoreSyncWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SaleRepositoryImpl(
    private val saleDao: SaleDao,
    private val productDao: ProductDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val context: Context
) : SaleRepository {

    private val syncRepository = SyncRepository(context)

    /** Returns the current user's UID, or null if not authenticated. */
    private fun uid(): String? = auth.currentUser?.uid

    // -------------------------------------------------------------------------
    // Reads — scoped to the authenticated user
    // -------------------------------------------------------------------------

    override fun getSalesByPeriod(start: Long, end: Long): Flow<List<Sale>> {
        val userId = uid() ?: return emptyFlow()
        return saleDao.getSalesByPeriod(userId, start, end)
    }

    override fun getAllSales(): Flow<List<Sale>> {
        val userId = uid() ?: return emptyFlow()
        return saleDao.getAllSales(userId)
    }

    override fun getTotalRevenue(): Flow<Double> {
        val userId = uid() ?: return emptyFlow()
        return saleDao.getTotalRevenue(userId).map { it ?: 0.0 }
    }

    // -------------------------------------------------------------------------
    // Write — stamp userId, validate, decrement stock, persist, sync
    // -------------------------------------------------------------------------

    /**
     * Validates and saves a sale (Req 2.4, 2.5, 2.6).
     * Room is written first (offline-first). Firestore sync is attempted
     * immediately and queued via WorkManager if offline.
     */
    override suspend fun insertSale(sale: Sale): Result<Unit> {
        val userId = uid()
            ?: return Result.failure(IllegalStateException("User not authenticated"))

        // Req 2.6: reject zero or negative quantity
        if (sale.quantity <= 0) {
            return Result.failure(IllegalArgumentException("Quantity must be greater than zero"))
        }

        // Fetch current variant (no userId filter — variantId is globally unique)
        val variant = productDao.getVariantById(sale.variantId)
            ?: return Result.failure(IllegalArgumentException("Variant not found"))

        // Req 2.5: oversell guard
        if (sale.quantity > variant.stock) {
            return Result.failure(
                IllegalArgumentException(
                    "Insufficient stock: requested ${sale.quantity}, available ${variant.stock}"
                )
            )
        }

        // Stamp userId on the sale entity
        val stampedSale = sale.copy(userId = userId)

        // Decrement stock in Room (offline-first)
        val newStock = variant.stock - sale.quantity
        productDao.updateStock(sale.variantId, newStock)

        // Persist sale in Room
        saleDao.insertSale(stampedSale)

        // Req 5.1: check low stock and fire local notification
        val updatedVariant = productDao.getVariantById(sale.variantId)
        if (updatedVariant != null) {
            val lowStock = AlertHelper.checkLowStock(listOf(updatedVariant))
            if (lowStock.isNotEmpty()) AlertHelper.notifyLowStock(context, lowStock)
        }

        // Sync sale to Firestore under users/{uid}/sales/{saleId}
        syncToFirestore(FirestoreSyncWorker.TYPE_SALE, stampedSale.id) {
            firestore.collection("users").document(userId)
                .collection("sales").document(stampedSale.id)
                .set(stampedSale.toMap()).await()
        }

        // Sync updated stock variant
        syncToFirestore(FirestoreSyncWorker.TYPE_VARIANT, sale.variantId) {
            val v = productDao.getVariantById(sale.variantId) ?: return@syncToFirestore
            firestore.collection("users").document(userId)
                .collection("product_variants").document(sale.variantId)
                .set(v.toMap()).await()
        }

        return Result.success(Unit)
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
