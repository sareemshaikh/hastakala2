package com.hastakala.testshop.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hastakala.testshop.repository.SyncRepository

/**
 * WorkManager worker that drains the local sync queue to Firestore.
 *
 * Triggered either:
 * - Immediately after a failed Firestore write (one-time, unique work)
 * - Periodically every 15 minutes (periodic work)
 *
 * Returns Result.retry() if any items failed, so WorkManager retries with
 * exponential backoff. Returns Result.success() when the queue is empty or
 * all items were pushed successfully.
 */
class FirestoreSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        // Entity type constants — used as the `type` field in SyncQueueItem
        const val TYPE_PRODUCT = "product"
        const val TYPE_VARIANT = "variant"
        const val TYPE_SALE = "sale"
        const val TYPE_DELETE_PRODUCT = "delete_product"
        const val TYPE_DELETE_VARIANT = "delete_variant"

        // Legacy input data keys kept for backward compatibility
        const val KEY_TYPE = "type"
        const val KEY_ENTITY_ID = "entity_id"
    }

    override suspend fun doWork(): Result {
        val syncRepository = SyncRepository(applicationContext)

        // If queue is empty, nothing to do
        if (syncRepository.pendingCount() == 0) return Result.success()

        // Drain the queue — returns false if any item failed
        val allSynced = syncRepository.drainQueue()
        return if (allSynced) Result.success() else Result.retry()
    }
}
