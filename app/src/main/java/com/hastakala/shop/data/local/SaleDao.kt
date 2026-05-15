package com.hastakala.testshop.data.local

import androidx.room.*
import com.hastakala.testshop.model.Sale
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: Sale)

    /** Returns all sales for the given user, newest first. */
    @Query("SELECT * FROM sales WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllSales(userId: String): Flow<List<Sale>>

    /**
     * Returns sales for the given user whose timestamp falls within [start, end].
     * Used by the Income Log period filters (Today / This Week / This Month).
     */
    @Query("""
        SELECT * FROM sales
        WHERE userId = :userId
          AND timestamp >= :start
          AND timestamp <= :end
        ORDER BY timestamp DESC
    """)
    fun getSalesByPeriod(userId: String, start: Long, end: Long): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE id = :saleId")
    suspend fun getSaleById(saleId: String): Sale?

    /** Total revenue for the given user. */
    @Query("SELECT SUM(totalAmount) FROM sales WHERE userId = :userId")
    fun getTotalRevenue(userId: String): Flow<Double?>
}
