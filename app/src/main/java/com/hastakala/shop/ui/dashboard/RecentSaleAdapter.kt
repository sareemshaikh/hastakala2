package com.hastakala.testshop.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hastakala.testshop.databinding.ItemRecentSaleBinding
import com.hastakala.testshop.model.Sale
import com.hastakala.testshop.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecentSaleAdapter : ListAdapter<Sale, RecentSaleAdapter.ViewHolder>(DIFF) {

    private val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    inner class ViewHolder(private val binding: ItemRecentSaleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(sale: Sale) {
            binding.tvSaleLabel.text = "${sale.productName} – ${sale.variantLabel}"
            binding.tvSaleQty.text = "×${sale.quantity}"
            binding.tvSaleAmount.text = CurrencyUtils.format(sale.totalAmount)
            binding.tvSaleTime.text = dateFormat.format(Date(sale.timestamp))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecentSaleBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Sale>() {
            override fun areItemsTheSame(a: Sale, b: Sale) = a.id == b.id
            override fun areContentsTheSame(a: Sale, b: Sale) = a == b
        }
    }
}
