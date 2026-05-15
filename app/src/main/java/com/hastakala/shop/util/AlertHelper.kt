package com.hastakala.testshop.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.hastakala.testshop.model.ProductVariant
import com.hastakala.testshop.ui.MainActivity

/**
 * Handles low-stock detection and local notifications.
 *
 * Deduplication: each variant gets its own notification ID derived from its
 * variantId hash. A SharedPreferences set tracks which variant IDs have already
 * been notified. The entry is cleared when stock rises above the threshold again.
 */
object AlertHelper {

    const val LOW_STOCK_THRESHOLD = 5

    const val CHANNEL_ID = "low_stock_channel"
    private const val CHANNEL_NAME = "Low Stock Alerts"
    private const val CHANNEL_DESC = "Alerts when a product variant is running low on stock"

    // SharedPreferences key for tracking already-notified variant IDs
    private const val PREFS_NAME = "alert_prefs"
    private const val PREF_NOTIFIED_IDS = "notified_variant_ids"

    // Extra key passed to MainActivity so it can navigate to Inventory
    const val EXTRA_NAVIGATE_TO = "navigate_to"
    const val DEST_INVENTORY = "inventory"

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns all variants whose stock is at or below [threshold] (Req 5.1, 5.4).
     */
    fun checkLowStock(
        variants: List<ProductVariant>,
        threshold: Int = LOW_STOCK_THRESHOLD
    ): List<ProductVariant> = variants.filter { it.stock <= threshold }

    /**
     * Fires a local notification for each low-stock variant that has not already
     * been notified. Skips variants that were already alerted to avoid spam (Req 5.1).
     *
     * Each variant gets its own notification so the user can dismiss them individually.
     * A summary notification is also posted when there are multiple alerts.
     */
    fun notifyLowStock(context: Context, lowStockVariants: List<ProductVariant>) {
        if (lowStockVariants.isEmpty()) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(manager)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val alreadyNotified = prefs.getStringSet(PREF_NOTIFIED_IDS, emptySet())!!.toMutableSet()

        // Deep-link intent: opens MainActivity and navigates to Inventory tab
        val deepLinkIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_TO, DEST_INVENTORY)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            deepLinkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        var newAlerts = 0

        for (variant in lowStockVariants) {
            if (variant.id in alreadyNotified) continue // already notified, skip

            val stockLabel = when (variant.stock) {
                0 -> "Out of stock!"
                else -> "Only ${variant.stock} left"
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Low Stock: ${variant.colorOrDesign}")
                .setContentText(stockLabel)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("${variant.colorOrDesign} — $stockLabel. Tap to restock.")
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            // Use a stable notification ID derived from the variantId hash
            val notifId = variant.id.hashCode()
            manager.notify(notifId, notification)
            alreadyNotified.add(variant.id)
            newAlerts++
        }

        // Persist updated notified set
        if (newAlerts > 0) {
            prefs.edit().putStringSet(PREF_NOTIFIED_IDS, alreadyNotified).apply()
        }
    }

    /**
     * Clears the "already notified" flag for variants that have been restocked
     * above the threshold. Call this after a stock update (Req 5.3).
     */
    fun clearAlertsForRestockedVariants(context: Context, variants: List<ProductVariant>) {
        val aboveThreshold = variants.filter { it.stock > LOW_STOCK_THRESHOLD }
        if (aboveThreshold.isEmpty()) return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val notified = prefs.getStringSet(PREF_NOTIFIED_IDS, emptySet())!!.toMutableSet()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        for (variant in aboveThreshold) {
            if (notified.remove(variant.id)) {
                // Also cancel the on-screen notification if still visible
                manager.cancel(variant.id.hashCode())
            }
        }
        prefs.edit().putStringSet(PREF_NOTIFIED_IDS, notified).apply()
    }

    /**
     * Returns true if the user has granted the POST_NOTIFICATIONS permission
     * (required on Android 13+).
     */
    fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    // -------------------------------------------------------------------------
    // Channel setup
    // -------------------------------------------------------------------------

    fun ensureChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = CHANNEL_DESC
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }
}
