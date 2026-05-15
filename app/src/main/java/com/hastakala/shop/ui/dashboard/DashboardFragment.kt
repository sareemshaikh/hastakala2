package com.hastakala.testshop.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hastakala.testshop.R
import com.hastakala.testshop.data.local.AppDatabase
import com.hastakala.testshop.databinding.FragmentDashboardBinding
import com.hastakala.testshop.repository.ProductRepositoryImpl
import com.hastakala.testshop.repository.SaleRepositoryImpl
import com.hastakala.testshop.ui.auth.LoginActivity
import com.hastakala.testshop.viewmodel.DashboardViewModel
import com.hastakala.testshop.viewmodel.DashboardViewModelFactory
import com.hastakala.testshop.util.CurrencyUtils
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels {
        val db = AppDatabase.getInstance(requireContext())
        val saleRepo = SaleRepositoryImpl(
            saleDao = db.saleDao(),
            productDao = db.productDao(),
            firestore = FirebaseFirestore.getInstance(),
            auth = FirebaseAuth.getInstance(),
            context = requireContext()
        )
        val productRepo = ProductRepositoryImpl(
            productDao = db.productDao(),
            firestore = FirebaseFirestore.getInstance(),
            auth = FirebaseAuth.getInstance(),
            context = requireContext()
        )
        DashboardViewModelFactory(saleRepo, productRepo)
    }

    private lateinit var lowStockAdapter: LowStockAdapter
    private lateinit var recentSaleAdapter: RecentSaleAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Show shimmer while first data loads
        showShimmer()

        setupRecyclerViews()
        setupQuickActions()
        setupSwipeRefresh()
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        lowStockAdapter = LowStockAdapter()
        binding.rvLowStock.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = lowStockAdapter
        }

        recentSaleAdapter = RecentSaleAdapter()
        binding.rvRecentSales.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recentSaleAdapter
        }
    }

    private fun setupQuickActions() {
        // Navigate to Quick Bill tab
        binding.btnQuickBill.setOnClickListener {
            findNavController().navigate(R.id.quickBillFragment)
        }

        // Navigate to Inventory tab
        binding.btnInventory.setOnClickListener {
            findNavController().navigate(R.id.inventoryFragment)
        }

        // Logout — sign out from Firebase and clear Room DB so the next user
        // starts with a clean local cache (data is still safe in Firestore)
        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            // Clear Room on a background thread before navigating away
            val db = AppDatabase.getInstance(requireContext())
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                db.clearAllTables()
            }
            startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeColors(
            requireContext().getColor(R.color.artisan_primary)
        )
        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun showShimmer() {
        val shimmer = binding.shimmerLayout as? ShimmerFrameLayout ?: return
        shimmer.visibility = View.VISIBLE
        shimmer.startShimmer()
        binding.scrollContent.visibility = View.INVISIBLE
    }

    private fun hideShimmer() {
        val shimmer = binding.shimmerLayout as? ShimmerFrameLayout ?: return
        shimmer.stopShimmer()
        shimmer.visibility = View.GONE
        binding.scrollContent.visibility = View.VISIBLE
    }

    private fun observeViewModel() {
        // Req 7.1 — total sales count
        viewModel.totalSales.observe(viewLifecycleOwner) { count ->
            binding.tvTotalSales.text = count.toString()
        }

        // Req 7.2 — total revenue
        viewModel.totalRevenue.observe(viewLifecycleOwner) { revenue ->
            binding.tvTotalRevenue.text = CurrencyUtils.formatCompact(revenue)
        }

        // Total units sold
        viewModel.totalUnits.observe(viewLifecycleOwner) { units ->
            binding.tvTotalUnits.text = units.toString()
        }

        // Req 7.3 — best seller
        viewModel.bestSeller.observe(viewLifecycleOwner) { best ->
            if (best != null) {
                binding.tvBestSeller.text = best.label
                binding.tvBestSellerUnits.text = "${best.totalUnits} units"
            } else {
                binding.tvBestSeller.text = "—"
                binding.tvBestSellerUnits.text = ""
            }
        }

        // Req 7.4 — low stock banner
        viewModel.lowStockItems.observe(viewLifecycleOwner) { items ->
            binding.cardLowStock.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
            lowStockAdapter.submitList(items)
        }

        // Recent sales + empty state
        viewModel.recentSales.observe(viewLifecycleOwner) { sales ->
            hideShimmer() // data arrived — hide skeleton
            if (sales.isEmpty()) {
                binding.cardRecentSales.visibility = View.GONE
                binding.layoutEmptyState.visibility = View.VISIBLE
            } else {
                binding.cardRecentSales.visibility = View.VISIBLE
                binding.layoutEmptyState.visibility = View.GONE
                recentSaleAdapter.submitList(sales)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
