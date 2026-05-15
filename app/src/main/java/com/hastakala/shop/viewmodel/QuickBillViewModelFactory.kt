package com.hastakala.testshop.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hastakala.testshop.repository.ProductRepository
import com.hastakala.testshop.repository.SaleRepository

class QuickBillViewModelFactory(
    private val productRepository: ProductRepository,
    private val saleRepository: SaleRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QuickBillViewModel::class.java)) {
            return QuickBillViewModel(productRepository, saleRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
