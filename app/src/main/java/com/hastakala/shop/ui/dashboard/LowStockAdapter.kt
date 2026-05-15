package com.hastakala.testshop.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hastakala.testshop.databinding.ItemLowStockBinding
import com.hastakala.testshop.model.ProductVariant

class LowStockAdapter : ListAdapter<ProductVariant, LowStockAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val binding: ItemLowStockBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(variant: ProductVariant) {
            binding.tvVariantLabel.text = variant.colorOrDesign
            binding.tvStockCount.text = "${variant.stock} left"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLowStockBinding.inflate(
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
