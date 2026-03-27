package com.foodlogger.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foodlogger.data.repository.FoodLoggerRepository
import com.foodlogger.domain.model.InventoryItem
import com.foodlogger.domain.model.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val repository: FoodLoggerRepository
) : ViewModel() {

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filteredProducts = MutableStateFlow<List<Product>>(emptyList())
    val filteredProducts: StateFlow<List<Product>> = _filteredProducts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _storeNameByProductId = MutableStateFlow<Map<Int, String>>(emptyMap())
    val storeNameByProductId: StateFlow<Map<Int, String>> = _storeNameByProductId.asStateFlow()

    private val _inInventoryProductIds = MutableStateFlow<Set<Int>>(emptySet())
    val inInventoryProductIds: StateFlow<Set<Int>> = _inInventoryProductIds.asStateFlow()

    init {
        mergeDuplicateProductsByName()
        loadProducts()
        loadProductStores()
    }

    private fun mergeDuplicateProductsByName() {
        viewModelScope.launch {
            try {
                repository.mergeProductsWithSameName()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error merging products"
            }
        }
    }

    private fun loadProductStores() {
        viewModelScope.launch {
            try {
                repository.getAllInventory().collect { items ->
                    _storeNameByProductId.value = buildStoreNameMap(items)
                    _inInventoryProductIds.value = items.map { it.productId }.toSet()
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error loading product stores"
            }
        }
    }

    private fun buildStoreNameMap(items: List<InventoryItem>): Map<Int, String> {
        return items
            .groupBy { it.productId }
            .mapValues { (_, group) ->
                group
                    .sortedByDescending { it.dateBought ?: it.dateCreated }
                    .firstOrNull { !it.boughtFromStoreName.isNullOrBlank() }
                    ?.boughtFromStoreName
                    .orEmpty()
            }
            .filterValues { it.isNotBlank() }
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _isLoading.value = true
            var firstEmission = true
            try {
                repository.getAllProducts().collect { items ->
                    _products.value = items
                    searchProducts(_searchQuery.value)
                    if (firstEmission) {
                        _isLoading.value = false
                        firstEmission = false
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Unknown error loading products"
                _isLoading.value = false
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        searchProducts(query)
    }

    private fun searchProducts(query: String) {
        viewModelScope.launch {
            try {
                repository.searchProducts(query).collect { items ->
                    _filteredProducts.value = items.sortedBy { it.name }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error searching products"
            }
        }
    }

    suspend fun addProduct(
        barcode: String?,
        name: String,
        brand: String? = null,
        category: String? = null,
        kcal: Float? = null,
        protein: Float? = null,
        carbs: Float? = null,
        fat: Float? = null
    ) {
        try {
            repository.addProduct(
                Product(
                    barcode = barcode,
                    name = name,
                    brand = brand,
                    category = category
                )
            )
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Error adding product"
        }
    }

    suspend fun updateProduct(product: Product) {
        try {
            repository.updateProduct(product)
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Error updating product"
        }
    }

    suspend fun deleteProductSuspend(barcode: String) {
        try {
            repository.deleteProduct(barcode)
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Error deleting product"
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // Public convenience methods for UI (non-suspend wrappers)
    fun deleteProduct(barcode: String) {
        viewModelScope.launch {
            deleteProductSuspend(barcode)
        }
    }

    fun deleteProductById(id: Int) {
        viewModelScope.launch {
            try {
                repository.deleteProductById(id)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error deleting product"
            }
        }
    }

    fun reloadProducts() {
        loadProducts()
    }

    fun saveProduct(product: Product) {
        viewModelScope.launch {
            updateProduct(product)
        }
    }
}
