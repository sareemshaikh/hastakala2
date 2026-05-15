package com.hastakala.testshop.ui.incomelog

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hastakala.testshop.R
import com.hastakala.testshop.data.local.AppDatabase
import com.hastakala.testshop.databinding.FragmentIncomeLogBinding
import com.hastakala.testshop.repository.SaleRepositoryImpl
import com.hastakala.testshop.viewmodel.IncomePeriod
import com.hastakala.testshop.viewmodel.IncomeLogViewModel
import com.hastakala.testshop.viewmodel.IncomeLogViewModelFactory
import com.hastakala.testshop.util.CurrencyUtils
import com.hastakala.testshop.viewmodel.SortOrder

class IncomeLogFragment : Fragment() {

    private var _binding: FragmentIncomeLogBinding? = null
    private val binding get() = _binding!!

    private val viewModel: IncomeLogViewModel by viewModels {
        val db = AppDatabase.getInstance(requireContext())
        val saleRepo = SaleRepositoryImpl(
            saleDao = db.saleDao(),
            productDao = db.productDao(),
            firestore = FirebaseFirestore.getInstance(),
            auth = FirebaseAuth.getInstance(),
            context = requireContext()
        )
        IncomeLogViewModelFactory(saleRepo)
    }

    private lateinit var saleAdapter: SaleAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIncomeLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupPeriodToggle()
        setupSearch()
        setupSortButton()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        saleAdapter = SaleAdapter()
        binding.rvSales.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = saleAdapter
        }
    }

    private fun setupPeriodToggle() {
        binding.togglePeriod.check(R.id.btnToday)
        binding.togglePeriod.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val period = when (checkedId) {
                R.id.btnToday -> IncomePeriod.TODAY
                R.id.btnThisWeek -> IncomePeriod.THIS_WEEK
                R.id.btnThisMonth -> IncomePeriod.THIS_MONTH
                else -> IncomePeriod.TODAY
            }
            viewModel.selectPeriod(period)
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.setSearchQuery(s?.toString() ?: "")
            }
        })
    }

    private fun setupSortButton() {
        binding.btnSort.setOnClickListener {
            viewModel.toggleSort()
        }
    }

    private fun observeViewModel() {
        // Filtered + sorted sales list (Req 6.1–6.4)
        viewModel.filteredSales.observe(viewLifecycleOwner) { sales ->
            saleAdapter.submitList(sales)
            val isEmpty = sales.isEmpty()
            binding.rvSales.visibility = if (isEmpty) View.GONE else View.VISIBLE
            binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        }

        // Period revenue total (Req 6.2–6.4)
        viewModel.periodRevenue.observe(viewLifecycleOwner) { revenue ->
            binding.tvPeriodRevenue.text = CurrencyUtils.format(revenue)
        }

        // Transaction count
        viewModel.transactionCount.observe(viewLifecycleOwner) { count ->
            binding.tvTransactionCount.text = count.toString()
        }

        // Sort button label reflects current order
        viewModel.sortOrder.observe(viewLifecycleOwner) { order ->
            binding.btnSort.text = when (order) {
                SortOrder.NEWEST_FIRST -> "↓ Newest"
                SortOrder.OLDEST_FIRST -> "↑ Oldest"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
