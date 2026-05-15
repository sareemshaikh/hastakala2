package com.hastakala.testshop.viewmodel

import androidx.lifecycle.*
import com.hastakala.testshop.model.ProductVariant
import com.hastakala.testshop.model.Sale
import com.hastakala.testshop.repository.ProductRepository
import com.hastakala.testshop.repository.SaleRepository
import com.hastakala.testshop.util.AlertHelper
import kotlinx.coroutines.flow.map

data class BestSeller(
    val variantId: String,
    val label: String,
    val totalUnits: Int
)

class DashboardViewModel(
    private val saleRepository: SaleRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    /** Total number of sale records (Req 7.1). */
    val totalSales: LiveData<Int> =
        saleRepository.getAllSales()
            .map { it.size }
            .asLiveData()

    /** Sum of totalAmount across all sales (Req 7.2). */
    val totalRevenue: LiveData<Double> =
        saleRepository.getTotalRevenue()
            .asLiveData()

    /** Total units sold across all sales. */
    val totalUnits: LiveData<Int> =
        saleRepository.getAllSales()
            .map { sales -> sales.sumOf { it.quantity } }
            .asLiveData()

    /** Best-selling variant by total units (Req 7.3). */
    val bestSeller: LiveData<BestSeller?> =
        saleRepository.getAllSales()
            .map { sales -> computeBestSeller(sales) }
            .asLiveData()

    /** All variants at or below the low-stock threshold (Req 7.4). */
    val lowStockItems: LiveData<List<ProductVariant>> =
        productRepository.getAllVariants()
            .map { variants -> AlertHelper.checkLowStock(variants) }
            .asLiveData()

    /** 5 most recent sales for the recent activity list. */
    val recentSales: LiveData<List<Sale>> =
        saleRepository.getAllSales()
            .map { sales -> sales.take(5) }
            .asLiveData()

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    internal fun computeBestSeller(sales: List<Sale>): BestSeller? {
        if (sales.isEmpty()) return null
        val unitsByVariant = sales
            .groupBy { it.variantId }
            .mapValues { (_, s) -> s.sumOf { it.quantity } }
        val (topId, topUnits) = unitsByVariant.maxByOrNull { it.value } ?: return null
        val sample = sales.first { it.variantId == topId }
        return BestSeller(
            variantId = topId,
            label = "${sample.productName} – ${sample.variantLabel}",
            totalUnits = topUnits
        )
    }
}
