package com.hastakala.testshop.ui.analytics

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hastakala.testshop.data.local.AppDatabase
import com.hastakala.testshop.databinding.FragmentAnalyticsBinding
import com.hastakala.testshop.repository.SaleRepositoryImpl
import com.hastakala.testshop.viewmodel.AnalyticsViewModel
import com.hastakala.testshop.viewmodel.AnalyticsViewModelFactory

class AnalyticsFragment : Fragment() {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnalyticsViewModel by viewModels {
        val db = AppDatabase.getInstance(requireContext())
        val saleRepo = SaleRepositoryImpl(
            saleDao = db.saleDao(),
            productDao = db.productDao(),
            firestore = FirebaseFirestore.getInstance(),
            auth = FirebaseAuth.getInstance(),
            context = requireContext()
        )
        AnalyticsViewModelFactory(saleRepo)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPieChart()
        setupBarChart()
        observeViewModel()
    }

    // -------------------------------------------------------------------------
    // Chart setup
    // -------------------------------------------------------------------------

    private fun setupPieChart() {
        binding.pieChart.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 42f
            transparentCircleRadius = 47f
            setHoleColor(Color.WHITE)
            setUsePercentValues(true)
            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(11f)
            setDrawEntryLabels(false) // labels shown in legend instead

            // Legend below chart
            legend.apply {
                isEnabled = true
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                textSize = 11f
                xEntrySpace = 8f
                yEntrySpace = 4f
                isWordWrapEnabled = true
            }
        }
    }

    private fun setupBarChart() {
        binding.barChart.apply {
            description.isEnabled = false
            setFitBars(true)
            setDrawGridBackground(false)
            setDrawBarShadow(false)

            // X-axis at bottom with rotated labels to avoid overlap
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                labelRotationAngle = -30f
                textSize = 10f
            }

            // Left axis — whole numbers only
            axisLeft.apply {
                granularity = 1f
                axisMinimum = 0f
                setDrawGridLines(true)
            }

            axisRight.isEnabled = false

            legend.apply {
                isEnabled = true
                textSize = 12f
            }
        }
    }

    // -------------------------------------------------------------------------
    // Observation
    // -------------------------------------------------------------------------

    private fun observeViewModel() {
        // Req 4.4 — best seller card
        viewModel.bestSellerLabel.observe(viewLifecycleOwner) { label ->
            binding.tvBestSellerLabel.text = label ?: "—"
        }

        // Req 4.1 — pie chart; Req 4.5 — empty state
        viewModel.pieData.observe(viewLifecycleOwner) { pieData ->
            if (pieData == null) {
                showEmptyState()
            } else {
                hideEmptyState()
                pieData.setValueFormatter(PercentFormatter(binding.pieChart))
                binding.pieChart.data = pieData
                // Smooth spin-in animation
                binding.pieChart.animateY(900, Easing.EaseInOutQuad)
            }
        }

        // Req 4.2 — bar chart
        viewModel.barData.observe(viewLifecycleOwner) { barData ->
            if (barData != null) {
                binding.barChart.data = barData
                binding.barChart.animateY(700, Easing.EaseInOutQuart)
            }
        }

        // Update bar chart x-axis labels from short variant labels
        viewModel.variantTotals.observe(viewLifecycleOwner) { totals ->
            val labels = totals.map { it.shortLabel }
            binding.barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            binding.barChart.xAxis.labelCount = labels.size
            binding.barChart.invalidate()
        }
    }

    // -------------------------------------------------------------------------
    // Empty state helpers (Req 4.5)
    // -------------------------------------------------------------------------

    private fun showEmptyState() {
        binding.layoutEmptyState.visibility = View.VISIBLE
        binding.cardBestSeller.visibility = View.GONE
        binding.cardPieChart.visibility = View.GONE
        binding.cardBarChart.visibility = View.GONE
    }

    private fun hideEmptyState() {
        binding.layoutEmptyState.visibility = View.GONE
        binding.cardBestSeller.visibility = View.VISIBLE
        binding.cardPieChart.visibility = View.VISIBLE
        binding.cardBarChart.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
