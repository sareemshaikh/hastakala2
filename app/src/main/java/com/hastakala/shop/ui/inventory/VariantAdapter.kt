package com.hastakala.testshop.ui.inventory

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hastakala.testshop.databinding.ItemVariantBinding
import com.hastakala.testshop.model.ProductVariant
import com.hastakala.testshop.util.CurrencyUtils
import com.hastakala.testshop.util.AlertHelper

class VariantAdapter(
    private val onEditStock: (ProductVariant) -> Unit
) : ListAdapter<ProductVariant, VariantAdapter.VariantViewHolder>(DIFF) {

    inner class VariantViewHolder(private val binding: ItemVariantBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(variant: ProductVariant) {
            binding.tvVariantLabel.text = variant.colorOrDesign
            binding.tvVariantStock.text = "Stock: ${variant.stock}"
            binding.tvVariantPrice.text = CurrencyUtils.format(variant.unitPrice)
            binding.btnEditStock.setOnClickListener { onEditStock(variant) }

            // Visual stock status badge
            val (label, color) = when {
                variant.stock == 0 -> "Out of Stock" to Color.parseColor("#D32F2F")
                variant.stock <= AlertHelper.LOW_STOCK_THRESHOLD -> "Low Stock" to Color.parseColor("#F57C00")
                else -> "In Stock" to Color.parseColor("#388E3C")
            }
            binding.tvStockStatus.text = label
            // Tint the rounded badge background
            val bg = binding.tvStockStatus.background.mutate() as GradientDrawable
            bg.setColor(color)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VariantViewHolder {
        val binding = ItemVariantBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VariantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VariantViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ProductVariant>() {
            override fun areItemsTheSame(a: ProductVariant, b: ProductVariant) = a.id == b.id
            override fun areContentsTheSame(a: ProductVariant, b: ProductVariant) = a == b
        }
    }
}
