package com.foodlogger.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foodlogger.data.repository.FoodLoggerRepository
import com.foodlogger.domain.model.ExpiryStatus
import com.foodlogger.domain.model.InventoryItem
import com.foodlogger.domain.model.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val repository: FoodLoggerRepository
) : ViewModel() {

    private val _inventoryItems = MutableStateFlow<List<InventoryItem>>(emptyList())
    val inventoryItems: StateFlow<List<InventoryItem>> = _inventoryItems.asStateFlow()

    private val _filteredItems = MutableStateFlow<List<InventoryItem>>(emptyList())
    val filteredItems: StateFlow<List<InventoryItem>> = _filteredItems.asStateFlow()

    private val _availableProducts = MutableStateFlow<List<Product>>(emptyList())
    val availableProducts: StateFlow<List<Product>> = _availableProducts.asStateFlow()

    private val _sortBy = MutableStateFlow<SortOption>(SortOption.EXPIRY_STATUS)
    val sortBy: StateFlow<SortOption> = _sortBy.asStateFlow()

    private val _filterByStatus = MutableStateFlow<ExpiryStatus?>(null)
    val filterByStatus: StateFlow<ExpiryStatus?> = _filterByStatus.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    enum class SortOption {
        EXPIRY_STATUS,
        ALPHABETICAL,
        DATE_BOUGHT,
        ALMOST_FINISHED
    }

    init {
        loadInventory()
        loadProducts()
    }

    private fun loadInventory() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getAllInventory().collect { items ->
                    _inventoryItems.value = items
                    applyFilterAndSort()
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Unknown error loading inventory"
            } finally {
                _isLoading.value = false
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

    fun setSortOption(option: SortOption) {
        _sortBy.value = option
        applyFilterAndSort()
    }

    fun setStatusFilter(status: ExpiryStatus?) {
        _filterByStatus.value = status
        applyFilterAndSort()
    }

    private fun applyFilterAndSort() {
        var filtered = _inventoryItems.value
        
        // Apply filter
        _filterByStatus.value?.let { status ->
            filtered = filtered.filter { it.expiryStatus == status }
        }

        // Apply sort
        filtered = when (_sortBy.value) {
            SortOption.EXPIRY_STATUS -> filtered.sortedWith(
                compareBy<InventoryItem> { it.expiryStatus.ordinal }
                    .thenBy { it.expiryDate }
            )
            SortOption.ALPHABETICAL -> filtered.sortedBy { it.displayName() }
            SortOption.DATE_BOUGHT -> filtered.sortedByDescending { it.dateBought }
            SortOption.ALMOST_FINISHED -> {
                filtered.sortedWith(
                    compareBy<InventoryItem> { !it.almostFinished }
                        .thenBy { it.expiryStatus.ordinal }
                )
            }
        }

        _filteredItems.value = filtered
    }

    suspend fun updateInventoryItem(
        id: Int,
        quantity: Float,
        expiryDate: LocalDateTime?,
        storageLocation: String?,
        nameOverride: String?,
        almostFinished: Boolean
    ) {
        try {
            repository.updateInventoryItem(
                id = id,
                quantity = quantity,
                expiryDate = expiryDate,
                storageLocation = storageLocation,
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
        barcode: String,
        quantity: Float,
        unit: String,
        expiryDate: LocalDateTime?,
        dateBought: LocalDateTime? = LocalDateTime.now(),
        storageLocation: String? = null,
        nameOverride: String? = null,
    ) {
        try {
            repository.addInventoryItem(
                barcode = barcode,
                quantity = quantity,
                unit = unit,
                dateBought = dateBought,
                expiryDate = expiryDate,
                storageLocation = storageLocation,
                nameOverride = nameOverride
            )
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Error adding item"
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // Public convenience methods for UI (non-suspend wrappers)
    fun sortItemsBy(option: SortOption) {
        setSortOption(option)
    }

    fun filterByStatus(status: ExpiryStatus?) {
        setStatusFilter(status)
    }

    fun deleteItem(id: Int) {
        viewModelScope.launch {
            deleteInventoryItem(id)
        }
    }

    fun reloadInventory() {
        loadInventory()
    }

    fun saveInventoryItem(
        item: InventoryItem,
        quantity: Float,
        storageLocation: String?,
        nameOverride: String?,
        almostFinished: Boolean
    ) {
        viewModelScope.launch {
            updateInventoryItem(
                id = item.id,
                quantity = quantity,
                expiryDate = item.expiryDate,
                storageLocation = storageLocation,
                nameOverride = nameOverride,
                almostFinished = almostFinished
            )
        }
    }

    fun createInventoryItem(
        barcode: String,
        quantity: Float,
        unit: String,
        expiryDate: LocalDateTime?,
        dateBought: LocalDateTime?,
        storageLocation: String?,
        nameOverride: String?
    ) {
        viewModelScope.launch {
            addInventoryItem(
                barcode = barcode,
                quantity = quantity,
                unit = unit,
                expiryDate = expiryDate,
                dateBought = dateBought,
                storageLocation = storageLocation,
                nameOverride = nameOverride
            )
        }
    }
}
