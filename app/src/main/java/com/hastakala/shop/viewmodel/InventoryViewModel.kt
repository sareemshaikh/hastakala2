package com.hastakala.testshop.viewmodel

import androidx.lifecycle.*
import com.hastakala.testshop.model.Product
import com.hastakala.testshop.model.ProductVariant
import com.hastakala.testshop.model.ProductWithVariants
import com.hastakala.testshop.repository.ProductRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

sealed class InventoryResult {
    object Success : InventoryResult()
    data class Error(val message: String) : InventoryResult()
}

class InventoryViewModel(
    private val productRepository: ProductRepository
) : ViewModel() {

    /** Current search query — empty string means show all. */
    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    // All products combined with their variants
    private val allProducts: LiveData<List<ProductWithVariants>> =
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

    /** Filtered product list based on search query. */
    val productList: LiveData<List<ProductWithVariants>> =
        allProducts.switchMap { list ->
            _searchQuery.map { query ->
                if (query.isBlank()) list
                else list.filter { it.product.name.contains(query, ignoreCase = true) }
            }
        }

    private val _operationResult = MutableLiveData<InventoryResult>()
    val operationResult: LiveData<InventoryResult> = _operationResult

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /** Add a new product with an initial variant. Rejects empty product names (Req 3.5). */
    fun addProduct(
        name: String,
        colorOrDesign: String,
        initialStock: Int,
        unitPrice: Double
    ) {
        if (name.isBlank()) {
            _operationResult.value = InventoryResult.Error("Product name cannot be empty")
            return
        }
        if (colorOrDesign.isBlank()) {
            _operationResult.value = InventoryResult.Error("Color/Design cannot be empty")
            return
        }
        if (initialStock < 0) {
            _operationResult.value = InventoryResult.Error("Stock cannot be negative")
            return
        }
        if (unitPrice <= 0) {
            _operationResult.value = InventoryResult.Error("Unit price must be greater than zero")
            return
        }
        viewModelScope.launch {
            try {
                val productId = UUID.randomUUID().toString()
                // userId is stamped by ProductRepositoryImpl.insertProduct()
                val product = Product(id = productId, userId = "", name = name.trim())
                val variant = ProductVariant(
                    id = UUID.randomUUID().toString(),
                    userId = "",  // stamped by repository
                    productId = productId,
                    colorOrDesign = colorOrDesign.trim(),
                    stock = initialStock,
                    unitPrice = unitPrice
                )
                productRepository.insertProduct(product)
                productRepository.insertVariant(variant)
                _operationResult.value = InventoryResult.Success
            } catch (e: Exception) {
                _operationResult.value = InventoryResult.Error(e.message ?: "Failed to add product")
            }
        }
    }

    /** Rename an existing product. */
    fun editProduct(productId: String, newName: String) {
        if (newName.isBlank()) {
            _operationResult.value = InventoryResult.Error("Product name cannot be empty")
            return
        }
        viewModelScope.launch {
            try {
                val product = Product(id = productId, userId = "", name = newName.trim())
                productRepository.insertProduct(product) // REPLACE strategy updates in place
                _operationResult.value = InventoryResult.Success
            } catch (e: Exception) {
                _operationResult.value = InventoryResult.Error(e.message ?: "Failed to edit product")
            }
        }
    }

    /** Add a new variant to an existing product. */
    fun addVariant(productId: String, colorOrDesign: String, stock: Int, unitPrice: Double) {
        viewModelScope.launch {
            try {
                val variant = ProductVariant(
                    id = UUID.randomUUID().toString(),
                    userId = "",  // stamped by repository
                    productId = productId,
                    colorOrDesign = colorOrDesign.trim(),
                    stock = stock,
                    unitPrice = unitPrice
                )
                productRepository.insertVariant(variant)
                _operationResult.value = InventoryResult.Success
            } catch (e: Exception) {
                _operationResult.value = InventoryResult.Error(e.message ?: "Failed to add variant")
            }
        }
    }

    /** Update the stock quantity of an existing variant (Req 3.2). */
    fun updateStock(variantId: String, newQty: Int) {
        viewModelScope.launch {
            try {
                productRepository.updateStock(variantId, newQty)
                _operationResult.value = InventoryResult.Success
            } catch (e: Exception) {
                _operationResult.value = InventoryResult.Error(e.message ?: "Failed to update stock")
            }
        }
    }

    /** Delete a product and all its variants (Req 3.3). */
    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            try {
                productRepository.deleteProduct(productId)
                _operationResult.value = InventoryResult.Success
            } catch (e: Exception) {
                _operationResult.value = InventoryResult.Error(e.message ?: "Failed to delete product")
            }
        }
    }
}
