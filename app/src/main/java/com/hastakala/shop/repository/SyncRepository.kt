package com.hastakala.testshop.repository

import android.content.Context
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hastakala.testshop.data.local.AppDatabase
import com.hastakala.testshop.model.SyncQueueItem
import com.hastakala.testshop.util.toMap
import com.hastakala.testshop.worker.FirestoreSyncWorker
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * Central repository for offline-first Firestore synchronisation.
 *
 * Responsibilities:
 * 1. Enqueue failed writes into the local Room sync_queue table.
 * 2. Drain the queue when connectivity is available.
 * 3. Schedule a periodic WorkManager job to drain the queue in the background.
 * 4. Prevent duplicate queue entries via composite PK "{type}_{entityId}".
 */
class SyncRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val syncQueueDao = db.syncQueueDao()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // -------------------------------------------------------------------------
    // Enqueue helpers — called by repositories on Firestore failure
    // -------------------------------------------------------------------------

    /** Add a pending write to the local queue. Duplicate entries are replaced (idempotent). */
    suspend fun enqueue(type: String, entityId: String) {
        val item = SyncQueueItem(
            id = "${type}_${entityId}",   // composite key prevents duplicates
            type = type,
            entityId = entityId
        )
        syncQueueDao.enqueue(item)
        scheduleImmediateSync()
    }

    /** Returns the number of items waiting to be synced. */
    suspend fun pendingCount(): Int = syncQueueDao.pendingCount()

    // -------------------------------------------------------------------------
    // Drain — process all queued items now
    // -------------------------------------------------------------------------

    /**
     * Attempts to push all queued items to Firestore.
     * Called by [FirestoreSyncWorker] when connectivity is available.
     * Returns true if all items were synced successfully.
     */
    suspend fun drainQueue(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val items = syncQueueDao.getAll()
        if (items.isEmpty()) return true

        val userDoc = firestore.collection("users").document(uid)
        var allSucceeded = true

        for (item in items) {
            val success = trySyncItem(item, userDoc.path)
            if (success) {
                syncQueueDao.removeById(item.id)
            } else {
                allSucceeded = false
            }
        }
        return allSucceeded
    }

    private suspend fun trySyncItem(item: SyncQueueItem, userPath: String): Boolean {
        return try {
            val userDoc = firestore.document(userPath)
            when (item.type) {
                FirestoreSyncWorker.TYPE_PRODUCT -> {
                    // Use the unscoped variant — entity already has userId stamped
                    val product = db.productDao().getProductById(item.entityId, auth.currentUser?.uid ?: return true) ?: return true
                    userDoc.collection("products").document(item.entityId).set(product.toMap()).await()
                }
                FirestoreSyncWorker.TYPE_VARIANT -> {
                    val variant = db.productDao().getVariantById(item.entityId) ?: return true
                    userDoc.collection("product_variants").document(item.entityId).set(variant.toMap()).await()
                }
                FirestoreSyncWorker.TYPE_SALE -> {
                    val sale = db.saleDao().getSaleById(item.entityId) ?: return true
                    userDoc.collection("sales").document(item.entityId).set(sale.toMap()).await()
                }
                FirestoreSyncWorker.TYPE_DELETE_PRODUCT -> {
                    userDoc.collection("products").document(item.entityId).delete().await()
                }
                FirestoreSyncWorker.TYPE_DELETE_VARIANT -> {
                    userDoc.collection("product_variants").document(item.entityId).delete().await()
                }
                else -> return true // unknown type — remove from queue
            }
            true
        } catch (e: Exception) {
            false // keep in queue for retry
        }
    }

    // -------------------------------------------------------------------------
    // WorkManager scheduling
    // -------------------------------------------------------------------------

    /**
     * Schedules a one-time sync job that runs as soon as network is available.
     * Uses a unique work name to prevent duplicate jobs.
     */
    fun scheduleImmediateSync() {
        val request = OneTimeWorkRequestBuilder<FirestoreSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME_IMMEDIATE,
            ExistingWorkPolicy.KEEP, // don't replace if already queued
            request
        )
    }

    /**
     * Schedules a periodic background sync every 15 minutes.
     * Call once from Application.onCreate().
     */
    fun schedulePeriodicSync() {
        val request = PeriodicWorkRequestBuilder<FirestoreSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    companion object {
        const val WORK_NAME_IMMEDIATE = "hastakala_sync_immediate"
        const val WORK_NAME_PERIODIC = "hastakala_sync_periodic"
    }
}
