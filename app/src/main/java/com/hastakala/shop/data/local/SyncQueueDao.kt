package com.hastakala.testshop.data.local

import androidx.room.*
import com.hastakala.testshop.model.SyncQueueItem

@Dao
interface SyncQueueDao {

    /** Insert or replace — the composite PK prevents duplicate entries for the same entity. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(item: SyncQueueItem)

    @Query("SELECT * FROM sync_queue ORDER BY enqueuedAt ASC")
    suspend fun getAll(): List<SyncQueueItem>

    @Query("SELECT COUNT(*) FROM sync_queue")
    suspend fun pendingCount(): Int

    @Delete
    suspend fun remove(item: SyncQueueItem)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun removeById(id: String)

    @Query("DELETE FROM sync_queue")
    suspend fun clearAll()
}
