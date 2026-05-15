package com.hastakala.testshop.viewmodel

import androidx.lifecycle.*
import com.hastakala.testshop.model.Sale
import com.hastakala.testshop.repository.SaleRepository
import kotlinx.coroutines.flow.map
import java.util.Calendar

enum class IncomePeriod { TODAY, THIS_WEEK, THIS_MONTH }
enum class SortOrder { NEWEST_FIRST, OLDEST_FIRST }

class IncomeLogViewModel(
    private val saleRepository: SaleRepository
) : ViewModel() {

    private val _selectedPeriod = MutableLiveData(IncomePeriod.TODAY)
    val selectedPeriod: LiveData<IncomePeriod> = _selectedPeriod

    private val _sortOrder = MutableLiveData(SortOrder.NEWEST_FIRST)
    val sortOrder: LiveData<SortOrder> = _sortOrder

    private val _searchQuery = MutableLiveData("")

    fun selectPeriod(period: IncomePeriod) { _selectedPeriod.value = period }
    fun toggleSort() {
        _sortOrder.value = if (_sortOrder.value == SortOrder.NEWEST_FIRST)
            SortOrder.OLDEST_FIRST else SortOrder.NEWEST_FIRST
    }
    fun setSearchQuery(query: String) { _searchQuery.value = query }

    /**
     * Sales filtered by period, search query, and sorted by the selected order.
     * Reacts to changes in period, sort, or search (Req 6.1–6.4).
     */
    val filteredSales: LiveData<List<Sale>> =
        // Combine period + sort + search into a single trigger
        MediatorLiveData<List<Sale>>().also { mediator ->
            // Raw period-filtered sales from Room
            val periodSales: LiveData<List<Sale>> = _selectedPeriod.switchMap { period ->
                val (start, end) = periodRange(period)
                saleRepository.getSalesByPeriod(start, end).asLiveData()
            }

            fun recompute() {
                val sales = periodSales.value ?: return
                val query = _searchQuery.value ?: ""
                val order = _sortOrder.value ?: SortOrder.NEWEST_FIRST

                val filtered = if (query.isBlank()) sales
                else sales.filter {
                    it.productName.contains(query, ignoreCase = true) ||
                    it.variantLabel.contains(query, ignoreCase = true)
                }

                mediator.value = if (order == SortOrder.NEWEST_FIRST)
                    filtered.sortedByDescending { it.timestamp }
                else
                    filtered.sortedBy { it.timestamp }
            }

            mediator.addSource(periodSales) { recompute() }
            mediator.addSource(_sortOrder) { recompute() }
            mediator.addSource(_searchQuery) { recompute() }
        }

    /** Total revenue for the currently filtered + searched set (Req 6.2–6.4). */
    val periodRevenue: LiveData<Double> = filteredSales.map { sales ->
        sales.sumOf { it.totalAmount }
    }

    /** Transaction count for the current filter. */
    val transactionCount: LiveData<Int> = filteredSales.map { it.size }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    companion object {
        /**
         * Returns [startEpochMillis, endEpochMillis] for the given period.
         * Exposed as companion so it can be tested independently.
         */
        fun periodRange(period: IncomePeriod): Pair<Long, Long> {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val endOfToday = cal.timeInMillis

            return when (period) {
                IncomePeriod.TODAY -> {
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    Pair(cal.timeInMillis, endOfToday)
                }
                IncomePeriod.THIS_WEEK -> {
                    cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    Pair(cal.timeInMillis, endOfToday)
                }
                IncomePeriod.THIS_MONTH -> {
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    Pair(cal.timeInMillis, endOfToday)
                }
            }
        }
    }
}
