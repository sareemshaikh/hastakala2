package com.hastakala.testshop.viewmodel

import androidx.lifecycle.*
import com.hastakala.testshop.model.ProductVariant
import com.hastakala.testshop.model.ProductWithVariants
import com.hastakala.testshop.model.Sale
import com.hastakala.testshop.repository.ProductRepository
import com.hastakala.testshop.repository.SaleRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.UUID

sealed class QuickBillResult {
    object Success : QuickBillResult()
    data class Error(val message: String) : QuickBillResult()
}

class QuickBillViewModel(
    private val productRepository: ProductRepository,
    private val saleRepository: SaleRepository
) : ViewModel() {

    /** Combined product + variant list for the product grid (Req 2.1). */
    val productList: LiveData<List<ProductWithVariants>> =
        productRepository.getAllProducts()
            .combine(productRepository.getAllVariants()) { products, allVariants ->
                products.map { product ->
                    ProductWithVariants(
                        product = product,
                        variants = allVariants.filter { it.productId == product.id }
                    )
                }
            }
            .asLiveData()

    /** Variants for the currently selected product (Req 2.2). */
    private val _selectedVariants = MutableLiveData<List<ProductVariant>>(emptyList())
    val selectedVariants: LiveData<List<ProductVariant>> = _selectedVariants

    private val _saleResult = MutableLiveData<QuickBillResult>()
    val saleResult: LiveData<QuickBillResult> = _saleResult

    /** Called when the artisan taps a product in the grid. */
    fun selectProduct(productWithVariants: ProductWithVariants) {
        _selectedVariants.value = productWithVariants.variants
    }

    /**
     * Validates and saves a sale (Req 2.3, 2.4, 2.5, 2.6).
     * - quantity must be > 0 (Req 2.6)
     * - quantity must not exceed variant stock (Req 2.5)
     * On success, stock is decremented and sale is persisted (Req 2.4).
     */
    fun saveSale(
        variant: ProductVariant,
        productName: String,
        quantity: Int
    ) {
        // Req 2.6: reject zero or negative quantity
        if (quantity <= 0) {
            _saleResult.value = QuickBillResult.Error("Quantity must be greater than zero")
            return
        }

        // Req 2.5: reject oversell
        if (quantity > variant.stock) {
            _saleResult.value = QuickBillResult.Error(
                "Insufficient stock: requested $quantity, available ${variant.stock}"
            )
            return
        }

        viewModelScope.launch {
            val sale = Sale(
                id = UUID.randomUUID().toString(),
                userId = "",  // stamped by SaleRepositoryImpl.insertSale()
                variantId = variant.id,
                productName = productName,
                variantLabel = variant.colorOrDesign,
                quantity = quantity,
                unitPrice = variant.unitPrice,
                totalAmount = quantity * variant.unitPrice,
                timestamp = System.currentTimeMillis()
            )
            val result = saleRepository.insertSale(sale)
            _saleResult.value = if (result.isSuccess) {
                QuickBillResult.Success
            } else {
                QuickBillResult.Error(
                    result.exceptionOrNull()?.message ?: "Failed to save sale"
                )
            }
        }
    }
}
