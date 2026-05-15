package com.hastakala.testshop.ui.quickbill

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hastakala.testshop.data.local.AppDatabase
import com.hastakala.testshop.databinding.FragmentQuickBillBinding
import com.hastakala.testshop.model.ProductVariant
import com.hastakala.testshop.repository.ProductRepositoryImpl
import com.hastakala.testshop.repository.SaleRepositoryImpl
import com.hastakala.testshop.viewmodel.QuickBillResult
import com.hastakala.testshop.viewmodel.QuickBillViewModel
import com.hastakala.testshop.viewmodel.QuickBillViewModelFactory
import com.hastakala.testshop.util.CurrencyUtils

class QuickBillFragment : Fragment() {

    private var _binding: FragmentQuickBillBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QuickBillViewModel by viewModels {
        val db = AppDatabase.getInstance(requireContext())
        val productRepo = ProductRepositoryImpl(
            productDao = db.productDao(),
            firestore = FirebaseFirestore.getInstance(),
            auth = FirebaseAuth.getInstance(),
            context = requireContext()
        )
        val saleRepo = SaleRepositoryImpl(
            saleDao = db.saleDao(),
            productDao = db.productDao(),
            firestore = FirebaseFirestore.getInstance(),
            auth = FirebaseAuth.getInstance(),
            context = requireContext()
        )
        QuickBillViewModelFactory(productRepo, saleRepo)
    }

    private lateinit var productGridAdapter: ProductGridAdapter
    private lateinit var variantSelectorAdapter: VariantSelectorAdapter

    private var selectedVariant: ProductVariant? = null
    private var selectedProductName: String = ""
    private var quantity: Int = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuickBillBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupProductGrid()
        setupVariantSelector()
        setupQuantityButtons()
        setupSaveButton()
        observeViewModel()
    }

    private fun setupProductGrid() {
        productGridAdapter = ProductGridAdapter { productWithVariants ->
            // Reset variant selection when a new product is chosen
            selectedProductName = productWithVariants.product.name
            selectedVariant = null
            variantSelectorAdapter.clearSelection()
            variantSelectorAdapter.submitList(productWithVariants.variants)
            viewModel.selectProduct(productWithVariants)
            updateTotalAndSaveButton()
        }
        binding.rvProducts.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = productGridAdapter
        }
    }

    private fun setupVariantSelector() {
        variantSelectorAdapter = VariantSelectorAdapter { variant ->
            selectedVariant = variant
            updateTotalAndSaveButton()
        }
        binding.rvVariants.apply {
            layoutManager = LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false
            )
            adapter = variantSelectorAdapter
        }
    }

    private fun setupQuantityButtons() {
        // Increment quantity
        binding.btnIncrement.setOnClickListener {
            val maxStock = selectedVariant?.stock ?: Int.MAX_VALUE
            if (quantity < maxStock) {
                quantity++
                binding.tvQuantity.text = quantity.toString()
                updateTotalAndSaveButton()
            }
        }

        // Decrement quantity — minimum is 1
        binding.btnDecrement.setOnClickListener {
            if (quantity > 1) {
                quantity--
                binding.tvQuantity.text = quantity.toString()
                updateTotalAndSaveButton()
            }
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            val variant = selectedVariant ?: return@setOnClickListener
            viewModel.saveSale(variant, selectedProductName, quantity)
        }
    }

    /**
     * Updates the total price card and save button state.
     * Save is enabled only when a variant is selected and quantity >= 1 (Req 2.3).
     */
    private fun updateTotalAndSaveButton() {
        val variant = selectedVariant
        if (variant != null) {
            val total = quantity * variant.unitPrice
            binding.tvTotal.text = CurrencyUtils.format(total)
            binding.cardTotal.visibility = View.VISIBLE
            binding.btnSave.isEnabled = true
        } else {
            binding.cardTotal.visibility = View.GONE
            binding.btnSave.isEnabled = false
        }
    }

    private fun observeViewModel() {
        viewModel.productList.observe(viewLifecycleOwner) { list ->
            productGridAdapter.submitList(list)
        }

        viewModel.saleResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is QuickBillResult.Success -> {
                    Snackbar.make(binding.root, "✓ Sale saved!", Snackbar.LENGTH_SHORT).show()
                    resetForm()
                }
                is QuickBillResult.Error -> {
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    /** Resets the form after a successful sale. */
    private fun resetForm() {
        quantity = 1
        binding.tvQuantity.text = "1"
        selectedVariant = null
        variantSelectorAdapter.clearSelection()
        binding.cardTotal.visibility = View.GONE
        binding.btnSave.isEnabled = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
