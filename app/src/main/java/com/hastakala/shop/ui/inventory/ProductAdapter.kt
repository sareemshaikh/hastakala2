package com.hastakala.testshop.ui.inventory

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hastakala.testshop.databinding.ItemProductBinding
import com.hastakala.testshop.model.ProductVariant
import com.hastakala.testshop.model.ProductWithVariants

class ProductAdapter(
    private val onAddVariant: (productId: String) -> Unit,
    private val onDeleteProduct: (productId: String) -> Unit,
    private val onEditProduct: (productId: String, currentName: String) -> Unit,
    private val onEditStock: (ProductVariant) -> Unit
) : ListAdapter<ProductWithVariants, ProductAdapter.ProductViewHolder>(DIFF) {

    inner class ProductViewHolder(private val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val variantAdapter = VariantAdapter(onEditStock)

        init {
            binding.rvVariants.apply {
                layoutManager = LinearLayoutManager(binding.root.context)
                adapter = variantAdapter
            }
        }

        fun bind(item: ProductWithVariants) {
            binding.tvProductName.text = item.product.name
            variantAdapter.submitList(item.variants)
            binding.btnAddVariant.setOnClickListener { onAddVariant(item.product.id) }
            binding.btnDeleteProduct.setOnClickListener { onDeleteProduct(item.product.id) }
            binding.btnEditProduct.setOnClickListener { onEditProduct(item.product.id, item.product.name) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ProductWithVariants>() {
            override fun areItemsTheSame(a: ProductWithVariants, b: ProductWithVariants) =
                a.product.id == b.product.id
            override fun areContentsTheSame(a: ProductWithVariants, b: ProductWithVariants) =
                a == b
        }
    }
}
