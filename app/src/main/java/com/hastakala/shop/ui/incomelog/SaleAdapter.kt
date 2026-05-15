package com.hastakala.testshop.ui.incomelog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hastakala.testshop.databinding.ItemSaleBinding
import com.hastakala.testshop.model.Sale
import com.hastakala.testshop.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SaleAdapter : ListAdapter<Sale, SaleAdapter.ViewHolder>(DIFF) {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    inner class ViewHolder(private val binding: ItemSaleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(sale: Sale) {
            // Product + variant label (Req 6.5)
            binding.tvProductVariant.text = "${sale.productName} – ${sale.variantLabel}"

            // Unit price and quantity (Req 6.5)
            binding.tvUnitPrice.text = CurrencyUtils.formatPerUnit(sale.unitPrice)
            binding.tvQuantity.text = "×${sale.quantity}"

            // Total amount (Req 6.5)
            binding.tvTotalAmount.text = CurrencyUtils.format(sale.totalAmount)

            // Formatted timestamp
            binding.tvTimestamp.text = dateFormat.format(Date(sale.timestamp))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSaleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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
