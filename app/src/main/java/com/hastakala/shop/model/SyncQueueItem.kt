package com.hastakala.testshop.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a pending Firestore write that failed due to being offline.
 * Stored in Room so it survives app restarts.
 * When connectivity is restored, FirestoreSyncWorker processes all pending items.
 */
@Entity(tableName = "sync_queue")
data class SyncQueueItem(
    @PrimaryKey val id: String,   // Composite key: "{type}_{entityId}" — prevents duplicates
    val type: String,             // "product" | "variant" | "sale" | "delete_product" | "delete_variant"
    val entityId: String,
    val enqueuedAt: Long = System.currentTimeMillis()
)
