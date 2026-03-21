package com.foodlogger.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foodlogger.data.db.ReceiptEntity
import com.foodlogger.data.repository.FoodLoggerRepository
import com.foodlogger.domain.model.InventoryItem
import com.foodlogger.domain.model.Product
import com.foodlogger.domain.model.Store
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val repository: FoodLoggerRepository
) : ViewModel() {

    private val _inventoryItems = MutableStateFlow<List<InventoryItem>>(emptyList())
    val inventoryItems: StateFlow<List<InventoryItem>> = _inventoryItems.asStateFlow()

    private val _availableProducts = MutableStateFlow<List<Product>>(emptyList())
    val availableProducts: StateFlow<List<Product>> = _availableProducts.asStateFlow()

    private val _availableStorageLocations = MutableStateFlow<List<String>>(emptyList())
    val availableStorageLocations: StateFlow<List<String>> = _availableStorageLocations.asStateFlow()

    private val _availableStores = MutableStateFlow<List<Store>>(emptyList())
    val availableStores: StateFlow<List<Store>> = _availableStores.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadInventory()
        loadProducts()
        loadStorageLocations()
        loadStores()
    }

    private fun loadStores() {
        viewModelScope.launch {
            try {
                repository.getAllStores().collect { stores ->
                    _availableStores.value = stores
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error loading stores"
            }
        }
    }

    private fun loadInventory() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getAllInventory().take(1).collect { items ->
                    _inventoryItems.value = items
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Unknown error loading inventory"
            } finally {
                _isLoading.value = false
            }
        }
        viewModelScope.launch {
            repository.getAllInventory().collect { items ->
                _inventoryItems.value = items
            }
        }
    }

    private fun loadProducts() {
        viewModelScope.launch {
            try {
                repository.getAllProducts().collect { products ->
                    _availableProducts.value = products.sortedBy { it.name }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error loading products"
            }
        }
    }

    private fun loadStorageLocations() {
        viewModelScope.launch {
            try {
                repository.getAllStorageLocations().collect { locations ->
                    _availableStorageLocations.value = locations.map { it.name }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error loading storage locations"
            }
        }
    }

    suspend fun updateInventoryItem(
        id: Int,
        quantity: Float,
        unit: String,
        expiryDate: LocalDateTime?,
        storageLocation: String?,
        boughtFromStoreId: Int?,
        nameOverride: String?,
        almostFinished: Boolean
    ) {
        try {
            repository.updateInventoryItem(
                id = id,
                quantity = quantity,
                unit = unit,
                expiryDate = expiryDate,
                storageLocation = storageLocation,
                boughtFromStoreId = boughtFromStoreId,
                nameOverride = nameOverride,
                almostFinished = almostFinished
            )
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Error updating item"
        }
    }

    suspend fun deleteInventoryItem(id: Int) {
        try {
            repository.deleteInventoryItem(id)
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Error deleting item"
        }
    }

    suspend fun addInventoryItem(
        productId: Int,
        quantity: Float,
        unit: String,
        expiryDate: LocalDateTime?,
        dateBought: LocalDateTime? = LocalDateTime.now(),
        storageLocation: String? = null,
        boughtFromStoreId: Int? = null,
        nameOverride: String? = null,
        imageUri: String? = null
    ) {
        try {
            repository.addInventoryItem(
                productId = productId,
                quantity = quantity,
                unit = unit,
                dateBought = dateBought,
                expiryDate = expiryDate,
                storageLocation = storageLocation,
                boughtFromStoreId = boughtFromStoreId,
                nameOverride = nameOverride,
                imageUri = imageUri
            )
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Error adding item"
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun deleteItem(id: Int) {
        viewModelScope.launch {
            deleteInventoryItem(id)
        }
    }

    fun reloadInventory() {
        loadInventory()
    }

    fun forceRefresh() {
    }

    fun saveInventoryItem(
        item: InventoryItem,
        quantity: Float,
        unit: String,
        expiryDate: LocalDateTime?,
        storageLocation: String?,
        boughtFromStoreId: Int?,
        nameOverride: String?,
        almostFinished: Boolean
    ) {
        viewModelScope.launch {
            updateInventoryItem(
                id = item.id,
                quantity = quantity,
                unit = unit,
                expiryDate = expiryDate,
                storageLocation = storageLocation,
                boughtFromStoreId = boughtFromStoreId,
                nameOverride = nameOverride,
                almostFinished = almostFinished
            )
        }
    }

    fun createInventoryItem(
        productId: Int,
        quantity: Float,
        unit: String,
        expiryDate: LocalDateTime?,
        dateBought: LocalDateTime?,
        storageLocation: String?,
        boughtFromStoreId: Int?,
        nameOverride: String?,
        imageUri: String? = null
    ) {
        viewModelScope.launch {
            addInventoryItem(
                productId = productId,
                quantity = quantity,
                unit = unit,
                expiryDate = expiryDate,
                dateBought = dateBought,
                storageLocation = storageLocation,
                boughtFromStoreId = boughtFromStoreId,
                nameOverride = nameOverride,
                imageUri = imageUri
            )
        }
    }

    fun createInventoryItemWithProduct(
        product: Product,
        quantity: Float,
        unit: String,
        expiryDate: LocalDateTime?,
        dateBought: LocalDateTime?,
        storageLocation: String?,
        boughtFromStoreId: Int?,
        nameOverride: String?,
        imageUri: String?
    ) {
        viewModelScope.launch {
            try {
                repository.addProductWithInventory(
                    product = product,
                    quantity = quantity,
                    unit = unit,
                    dateBought = dateBought,
                    expiryDate = expiryDate,
                    storageLocation = storageLocation,
                    boughtFromStoreId = boughtFromStoreId,
                    nameOverride = nameOverride,
                    imageUri = imageUri
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error adding item"
            }
        }
    }

    suspend fun getReceiptById(id: Int): ReceiptEntity? {
        return repository.getReceiptById(id)
    }
}
