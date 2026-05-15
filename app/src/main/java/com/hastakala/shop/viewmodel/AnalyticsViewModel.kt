package com.hastakala.testshop.viewmodel

import android.graphics.Color
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.hastakala.testshop.model.Sale
import com.hastakala.testshop.repository.SaleRepository
import kotlinx.coroutines.flow.map

/**
 * Holds per-variant unit totals computed from all sales.
 * Used to populate both Pie and Bar charts (Req 4.1, 4.2).
 */
data class VariantSaleTotal(
    val variantId: String,
    val label: String,       // "ProductName – VariantLabel"
    val shortLabel: String,  // Shortened label for bar chart x-axis
    val totalUnits: Int
)

class AnalyticsViewModel(
    private val saleRepository: SaleRepository
) : ViewModel() {

    // Distinct colors for chart segments — more readable than ColorTemplate defaults
    private val chartColors = listOf(
        Color.parseColor("#6200EE"), // purple
        Color.parseColor("#03DAC5"), // teal
        Color.parseColor("#FF6D00"), // orange
        Color.parseColor("#2979FF"), // blue
        Color.parseColor("#00C853"), // green
        Color.parseColor("#D50000"), // red
        Color.parseColor("#AA00FF"), // deep purple
        Color.parseColor("#FFAB00")  // amber
    )

    /** Raw per-variant totals, sorted descending by units (Req 4.1, 4.2, 4.4). */
    val variantTotals: LiveData<List<VariantSaleTotal>> =
        saleRepository.getAllSales()
            .map { sales -> computeVariantTotals(sales) }
            .asLiveData()

    /** PieData for MPAndroidChart (Req 4.1). Null when no sales exist (Req 4.5). */
    val pieData: LiveData<PieData?> = variantTotals.map { totals ->
        if (totals.isEmpty()) null else buildPieData(totals)
    }

    /** BarData for MPAndroidChart (Req 4.2). Null when no sales exist (Req 4.5). */
    val barData: LiveData<BarData?> = variantTotals.map { totals ->
        if (totals.isEmpty()) null else buildBarData(totals)
    }

    /** Label of the best-selling variant, or null when no sales exist (Req 4.4). */
    val bestSellerLabel: LiveData<String?> = variantTotals.map { totals ->
        totals.firstOrNull()?.let { "${it.label} (${it.totalUnits} units)" }
    }

    // -------------------------------------------------------------------------
    // Internal helpers — internal visibility allows unit testing without Android
    // -------------------------------------------------------------------------

    internal fun computeVariantTotals(sales: List<Sale>): List<VariantSaleTotal> {
        if (sales.isEmpty()) return emptyList()
        return sales
            .groupBy { it.variantId }
            .map { (variantId, variantSales) ->
                val sample = variantSales.first()
                val fullLabel = "${sample.productName} – ${sample.variantLabel}"
                // Short label for bar chart x-axis (max 10 chars to avoid overlap)
                val shortLabel = if (sample.variantLabel.length <= 10)
                    sample.variantLabel
                else
                    sample.variantLabel.take(8) + "…"
                VariantSaleTotal(
                    variantId = variantId,
                    label = fullLabel,
                    shortLabel = shortLabel,
                    totalUnits = variantSales.sumOf { it.quantity }
                )
            }
            .sortedByDescending { it.totalUnits }
    }

    private fun buildPieData(totals: List<VariantSaleTotal>): PieData {
        val entries = totals.map { PieEntry(it.totalUnits.toFloat(), it.label) }
        val colors = totals.mapIndexed { i, _ -> chartColors[i % chartColors.size] }
        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            valueTextSize = 11f
            valueTextColor = Color.WHITE
            sliceSpace = 2f
        }
        return PieData(dataSet).apply {
            setValueTextSize(11f)
        }
    }

    private fun buildBarData(totals: List<VariantSaleTotal>): BarData {
        val entries = totals.mapIndexed { index, total ->
            BarEntry(index.toFloat(), total.totalUnits.toFloat())
        }
        val colors = totals.mapIndexed { i, _ -> chartColors[i % chartColors.size] }
        val dataSet = BarDataSet(entries, "Units Sold").apply {
            this.colors = colors
            valueTextSize = 11f
            valueTextColor = Color.BLACK
        }
        return BarData(dataSet).apply { barWidth = 0.6f }
    }
}
