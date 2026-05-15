package com.hastakala.testshop.ui.quickbill

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hastakala.testshop.databinding.ItemProductGridBinding
import com.hastakala.testshop.model.ProductWithVariants

class ProductGridAdapter(
    private val onProductSelected: (ProductWithVariants) -> Unit
) : ListAdapter<ProductWithVariants, ProductGridAdapter.ViewHolder>(DIFF) {

    private var selectedId: String? = null

    inner class ViewHolder(private val binding: ItemProductGridBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ProductWithVariants) {
            binding.tvProductName.text = item.product.name
            if (item.product.imageResId != null) {
                binding.ivProductIcon.setImageResource(item.product.imageResId)
            } else {
                binding.ivProductIcon.setImageResource(android.R.drawable.ic_menu_gallery)
            }
            // Highlight selected card
            binding.root.isSelected = item.product.id == selectedId
            binding.root.alpha = if (item.product.id == selectedId) 1.0f else 0.75f
            binding.root.setOnClickListener {
                selectedId = item.product.id
                notifyDataSetChanged()
                onProductSelected(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProductGridBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
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
