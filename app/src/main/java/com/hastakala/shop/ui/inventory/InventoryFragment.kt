package com.hastakala.testshop.ui.inventory

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hastakala.testshop.data.local.AppDatabase
import com.hastakala.testshop.databinding.DialogAddProductBinding
import com.hastakala.testshop.databinding.DialogAddVariantBinding
import com.hastakala.testshop.databinding.DialogEditStockBinding
import com.hastakala.testshop.databinding.FragmentInventoryBinding
import com.hastakala.testshop.model.ProductVariant
import com.hastakala.testshop.repository.ProductRepositoryImpl
import com.hastakala.testshop.viewmodel.InventoryResult
import com.hastakala.testshop.viewmodel.InventoryViewModel
import com.hastakala.testshop.viewmodel.InventoryViewModelFactory

class InventoryFragment : Fragment() {

    private var _binding: FragmentInventoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InventoryViewModel by viewModels {
        val db = AppDatabase.getInstance(requireContext())
        val repo = ProductRepositoryImpl(
            productDao = db.productDao(),
            firestore = FirebaseFirestore.getInstance(),
            auth = FirebaseAuth.getInstance(),
            context = requireContext()
        )
        InventoryViewModelFactory(repo)
    }

    private lateinit var productAdapter: ProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter()
        setupSearch()
        setupFab()
        observeViewModel()
    }

    private fun setupAdapter() {
        productAdapter = ProductAdapter(
            onAddVariant = { productId -> showAddVariantDialog(productId) },
            onDeleteProduct = { productId -> confirmDeleteProduct(productId) },
            onEditProduct = { productId, name -> showEditProductDialog(productId, name) },
            onEditStock = { variant -> showEditStockDialog(variant) }
        )
        binding.rvProducts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = productAdapter
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

    private fun setupFab() {
        binding.fabAddProduct.setOnClickListener { showAddProductDialog() }
    }

    private fun observeViewModel() {
        viewModel.productList.observe(viewLifecycleOwner) { list ->
            productAdapter.submitList(list)
            val count = list.size
            binding.tvProductCount.text = "$count product${if (count != 1) "s" else ""}"
            binding.layoutEmptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            binding.rvProducts.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.operationResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is InventoryResult.Error ->
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                is InventoryResult.Success -> { /* list updates automatically via Flow */ }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Dialogs
    // -------------------------------------------------------------------------

    private fun showAddProductDialog() {
        val dialogBinding = DialogAddProductBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Add Product")
            .setView(dialogBinding.root)
            .setPositiveButton("Add", null) // set manually to prevent auto-dismiss on error
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = dialogBinding.etProductName.text?.toString()?.trim() ?: ""
                val color = dialogBinding.etColorDesign.text?.toString()?.trim() ?: ""
                val stock = dialogBinding.etInitialStock.text?.toString()?.toIntOrNull()
                val price = dialogBinding.etUnitPrice.text?.toString()?.toDoubleOrNull()

                // Inline validation
                if (name.isBlank()) {
                    dialogBinding.etProductName.error = "Required"
                    return@setOnClickListener
                }
                if (color.isBlank()) {
                    dialogBinding.etColorDesign.error = "Required"
                    return@setOnClickListener
                }
                if (stock == null || stock < 0) {
                    dialogBinding.etInitialStock.error = "Enter a valid number"
                    return@setOnClickListener
                }
                if (price == null || price <= 0) {
                    dialogBinding.etUnitPrice.error = "Must be > 0"
                    return@setOnClickListener
                }
                viewModel.addProduct(name, color, stock, price)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun showEditProductDialog(productId: String, currentName: String) {
        val input = TextInputEditText(requireContext()).apply {
            setText(currentName)
            hint = "Product name"
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Edit Product Name")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text?.toString()?.trim() ?: ""
                viewModel.editProduct(productId, newName)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddVariantDialog(productId: String) {
        val dialogBinding = DialogAddVariantBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Add Variant")
            .setView(dialogBinding.root)
            .setPositiveButton("Add", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val color = dialogBinding.etColorDesign.text?.toString()?.trim() ?: ""
                val stock = dialogBinding.etStock.text?.toString()?.toIntOrNull()
                val price = dialogBinding.etUnitPrice.text?.toString()?.toDoubleOrNull()

                if (color.isBlank()) {
                    dialogBinding.etColorDesign.error = "Required"
                    return@setOnClickListener
                }
                if (stock == null || stock < 0) {
                    dialogBinding.etStock.error = "Enter a valid number"
                    return@setOnClickListener
                }
                if (price == null || price <= 0) {
                    dialogBinding.etUnitPrice.error = "Must be > 0"
                    return@setOnClickListener
                }
                viewModel.addVariant(productId, color, stock, price)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun showEditStockDialog(variant: ProductVariant) {
        val dialogBinding = DialogEditStockBinding.inflate(layoutInflater)
        dialogBinding.etNewStock.setText(variant.stock.toString())

        AlertDialog.Builder(requireContext())
            .setTitle("Update Stock — ${variant.colorOrDesign}")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val newQty = dialogBinding.etNewStock.text?.toString()?.toIntOrNull()
                if (newQty != null && newQty >= 0) {
                    viewModel.updateStock(variant.id, newQty)
                } else {
                    Snackbar.make(binding.root, "Enter a valid quantity (≥ 0)", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteProduct(productId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Product")
            .setMessage("This will remove the product and all its variants. This cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> viewModel.deleteProduct(productId) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
