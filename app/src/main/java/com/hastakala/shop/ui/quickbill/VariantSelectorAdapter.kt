package com.hastakala.testshop.ui.quickbill

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hastakala.testshop.util.CurrencyUtils
import com.hastakala.testshop.model.ProductVariant
import com.hastakala.testshop.databinding.ItemVariantSelectorBinding

class VariantSelectorAdapter(
    private val onVariantSelected: (ProductVariant) -> Unit
) : ListAdapter<ProductVariant, VariantSelectorAdapter.ViewHolder>(DIFF) {

    private var selectedId: String? = null

    /** Returns the currently selected variant, or null if none selected. */
    fun getSelectedVariant(): ProductVariant? =
        currentList.firstOrNull { it.id == selectedId }

    /** Clears the selection (called when a new product is chosen). */
    fun clearSelection() {
        selectedId = null
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemVariantSelectorBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(variant: ProductVariant) {
            binding.tvVariantLabel.text = variant.colorOrDesign
            binding.tvVariantStock.text = "Stock: ${variant.stock}"
            binding.tvVariantPrice.text = CurrencyUtils.format(variant.unitPrice)
            binding.root.isSelected = variant.id == selectedId
            binding.root.alpha = if (variant.id == selectedId) 1.0f else 0.75f
            binding.root.setOnClickListener {
                selectedId = variant.id
                notifyDataSetChanged()
                onVariantSelected(variant)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVariantSelectorBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ProductVariant>() {
            override fun areItemsTheSame(a: ProductVariant, b: ProductVariant) = a.id == b.id
            override fun areContentsTheSame(a: ProductVariant, b: ProductVariant) = a == b
        }
    }
}
