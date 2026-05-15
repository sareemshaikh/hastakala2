package com.hastakala.testshop.worker

import android.content.Context
import androidx.work.*
import com.hastakala.testshop.data.local.AppDatabase
import com.hastakala.testshop.util.AlertHelper
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Periodic WorkManager worker that checks all product variants for low stock
 * and fires notifications for any that are at or below the threshold.
 *
 * Runs every 6 hours when the device is not low on battery.
 * Deduplication is handled inside AlertHelper via SharedPreferences.
 */
class StockCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "hastakala_stock_check"

        /**
         * Schedules the periodic stock check. Call once from Application.onCreate().
         * Uses ExistingPeriodicWorkPolicy.KEEP so it is only registered once.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<StockCheckWorker>(6, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        // Skip if notifications are disabled by the user
        if (!AlertHelper.areNotificationsEnabled(applicationContext)) {
            return Result.success()
        }

        val db = AppDatabase.getInstance(applicationContext)
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ?: return Result.success() // no user logged in — nothing to check

        // Collect all variants for the current user (single snapshot)
        val allVariants = db.productDao().getAllVariants(uid).first()

        // Clear alerts for variants that have been restocked (Req 5.3)
        AlertHelper.clearAlertsForRestockedVariants(applicationContext, allVariants)

        // Check for low stock and notify
        val lowStock = AlertHelper.checkLowStock(allVariants)
        if (lowStock.isNotEmpty()) {
            AlertHelper.notifyLowStock(applicationContext, lowStock)
        }

        return Result.success()
    }
}
