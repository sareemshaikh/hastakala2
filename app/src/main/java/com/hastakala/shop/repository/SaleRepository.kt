package com.hastakala.testshop.repository

import com.hastakala.testshop.model.Sale
import kotlinx.coroutines.flow.Flow

interface SaleRepository {
    fun getSalesByPeriod(start: Long, end: Long): Flow<List<Sale>>
    suspend fun insertSale(sale: Sale): Result<Unit>
    fun getTotalRevenue(): Flow<Double>
    fun getAllSales(): Flow<List<Sale>>
}
