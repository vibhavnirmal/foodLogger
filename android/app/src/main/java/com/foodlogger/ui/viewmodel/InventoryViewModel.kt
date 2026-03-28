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

    private data class RestorePlacement(
        val signature: String,
        val preferredIndex: Int,
    )

    private val _inventoryItems = MutableStateFlow<List<InventoryItem>>(emptyList())
    val inventoryItems: StateFlow<List<InventoryItem>> = _inventoryItems.asStateFlow()

    private val _availableProducts = MutableStateFlow<List<Product>>(emptyList())
    val availableProducts: StateFlow<List<Product>> = _availableProducts.asStateFlow()

    private val _availableStorageLocations = MutableStateFlow<List<String>>(emptyList())
    val availableStorageLocations: StateFlow<List<String>> = _availableStorageLocations.asStateFlow()

    private val _availableStores = MutableStateFlow<List<Store>>(emptyList())
    val availableStores: StateFlow<List<Store>> = _availableStores.asStateFlow()

    private val _availableCategories = MutableStateFlow<List<String>>(emptyList())
    val availableCategories: StateFlow<List<String>> = _availableCategories.asStateFlow()

    private val _filterStoreId = MutableStateFlow<Int?>(null)
    val filterStoreId: StateFlow<Int?> = _filterStoreId.asStateFlow()

    private val _filterStorageLocation = MutableStateFlow<String?>(null)
    val filterStorageLocation: StateFlow<String?> = _filterStorageLocation.asStateFlow()

    private val _filterCategory = MutableStateFlow<String?>(null)
    val filterCategory: StateFlow<String?> = _filterCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortByExpiry = MutableStateFlow(false)
    val sortByExpiry: StateFlow<Boolean> = _sortByExpiry.asStateFlow()

    private val _filteredInventoryItems = MutableStateFlow<List<InventoryItem>>(emptyList())
    val filteredInventoryItems: StateFlow<List<InventoryItem>> = _filteredInventoryItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var restorePlacement: RestorePlacement? = null

    init {
        loadInventory()
        loadProducts()
        loadStorageLocations()
        loadStores()
        loadCategories()
        
        viewModelScope.launch {
            _inventoryItems.collect {
                applyFilters()
            }
        }
        viewModelScope.launch {
            _availableProducts.collect {
                applyFilters()
            }
        }
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

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                repository.getDistinctCategories().collect { categories ->
                    _availableCategories.value = categories
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error loading categories"
            }
        }
    }

    private fun loadInventory() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getAllInventory().take(1).collect { items ->
                    _inventoryItems.value = applyRestorePlacement(items)
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Unknown error loading inventory"
            } finally {
                _isLoading.value = false
            }
        }
        viewModelScope.launch {
            repository.getAllInventory().collect { items ->
                _inventoryItems.value = applyRestorePlacement(items)
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

    fun setFilterStoreId(storeId: Int?) {
        _filterStoreId.value = storeId
        applyFilters()
    }

    fun setFilterStorageLocation(location: String?) {
        _filterStorageLocation.value = location
        applyFilters()
    }

    fun setFilterCategory(category: String?) {
        _filterCategory.value = category
        applyFilters()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    fun setSortByExpiry(sort: Boolean) {
        _sortByExpiry.value = sort
        applyFilters()
    }

    private fun applyFilters() {
        val items = _inventoryItems.value
        val storeId = _filterStoreId.value
        val storageLocation = _filterStorageLocation.value
        val category = _filterCategory.value
        val query = _searchQuery.value.lowercase().trim()
        val sortByExpiry = _sortByExpiry.value
        
        val productCategoryMap = _availableProducts.value.associate { it.id to it.category }

        val filtered = items.filter { item ->
            // Store filter
            (storeId == null || item.boughtFromStoreId == storeId) &&
            // Storage location filter
            (storageLocation == null || item.storageLocation == storageLocation) &&
            // Category filter
            (category == null || 
                (category == "" && (productCategoryMap[item.productId].isNullOrEmpty())) ||
                (category != "" && productCategoryMap[item.productId] == category)) &&
            // Search query
            (query.isEmpty() ||
                item.displayName().lowercase().contains(query) ||
                item.productName.lowercase().contains(query) ||
                (item.barcode?.lowercase()?.contains(query) == true) ||
                (item.storageLocation?.lowercase()?.contains(query) == true) ||
                (item.boughtFromStoreName?.lowercase()?.contains(query) == true))
        }

        val sorted = if (sortByExpiry) {
            filtered.sortedWith(compareBy<InventoryItem> { item ->
                when {
                    item.expiryDate == null -> 3
                    item.expiryStatus == com.foodlogger.domain.model.ExpiryStatus.EXPIRED -> 0
                    item.expiryStatus == com.foodlogger.domain.model.ExpiryStatus.EXPIRING_SOON -> 1
                    else -> 2
                }
            }
                .thenBy { it.expiryDate ?: java.time.LocalDateTime.MAX }
                .thenBy { it.displayName().lowercase() }
                .thenBy { it.id })
        } else {
            filtered
        }

        _filteredInventoryItems.value = sorted
    }

    suspend fun updateInventoryItem(
        id: Int,
        expiryDate: LocalDateTime?,
        storageLocation: String?,
        boughtFromStoreId: Int?,
        nameOverride: String?,
        almostFinished: Boolean
    ) {
        try {
            repository.updateInventoryItem(
                id = id,
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

    fun deleteItem(item: InventoryItem, originalIndex: Int? = null) {
        _inventoryItems.value = _inventoryItems.value.filterNot { it.id == item.id }
        viewModelScope.launch {
            try {
                deleteInventoryItem(item.id)
            } catch (e: Exception) {
                _inventoryItems.value = insertAtPreferredIndex(_inventoryItems.value, item, originalIndex)
            }
        }
    }

    fun restoreDeletedItem(item: InventoryItem, originalIndex: Int? = null) {
        if (originalIndex != null) {
            restorePlacement = RestorePlacement(
                signature = inventorySignature(item),
                preferredIndex = originalIndex,
            )
        }
        if (_inventoryItems.value.none { it.id == item.id }) {
            _inventoryItems.value = insertAtPreferredIndex(_inventoryItems.value, item, originalIndex)
        }
        viewModelScope.launch {
            try {
                repository.restoreInventoryItem(item)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error restoring item"
                _inventoryItems.value = _inventoryItems.value.filterNot { it.id == item.id }
            }
        }
    }

    private fun insertAtPreferredIndex(
        current: List<InventoryItem>,
        item: InventoryItem,
        preferredIndex: Int?
    ): List<InventoryItem> {
        val withoutItem = current.filterNot { it.id == item.id }
        val mutable = withoutItem.toMutableList()
        val targetIndex = (preferredIndex ?: mutable.size).coerceIn(0, mutable.size)
        mutable.add(targetIndex, item)
        return mutable
    }

    private fun applyRestorePlacement(items: List<InventoryItem>): List<InventoryItem> {
        val placement = restorePlacement ?: return items
        val fromIndex = items.indexOfFirst { inventorySignature(it) == placement.signature }
        if (fromIndex < 0) return items

        val mutable = items.toMutableList()
        val restoredItem = mutable.removeAt(fromIndex)
        val targetIndex = placement.preferredIndex.coerceIn(0, mutable.size)
        mutable.add(targetIndex, restoredItem)
        return mutable
    }

    private fun inventorySignature(item: InventoryItem): String {
        return listOf(
            item.productId.toString(),
            item.displayName(),
            item.dateBought?.toString().orEmpty(),
            item.expiryDate?.toString().orEmpty(),
            item.storageLocation.orEmpty(),
            item.boughtFromStoreId?.toString().orEmpty(),
            item.nameOverride.orEmpty(),
            item.almostFinished.toString(),
            item.imageUri.orEmpty(),
            item.receiptId?.toString().orEmpty(),
            item.dateCreated.toString(),
        ).joinToString("|")
    }

    fun reloadInventory() {
        loadInventory()
    }

    fun forceRefresh() {
    }

    fun saveInventoryItem(
        item: InventoryItem,
        expiryDate: LocalDateTime?,
        storageLocation: String?,
        boughtFromStoreId: Int?,
        nameOverride: String?,
        almostFinished: Boolean
    ) {
        viewModelScope.launch {
            updateInventoryItem(
                id = item.id,
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
