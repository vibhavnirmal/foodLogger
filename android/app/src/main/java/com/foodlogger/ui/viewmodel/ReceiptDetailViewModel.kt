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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class ReceiptDetailViewModel @Inject constructor(
    private val repository: FoodLoggerRepository
) : ViewModel() {

    private val _receipt = MutableStateFlow<ReceiptEntity?>(null)
    val receipt: StateFlow<ReceiptEntity?> = _receipt.asStateFlow()

    private val _inventoryItems = MutableStateFlow<List<InventoryItem>>(emptyList())
    val inventoryItems: StateFlow<List<InventoryItem>> = _inventoryItems.asStateFlow()

    private val _availableStores = MutableStateFlow<List<Store>>(emptyList())
    val availableStores: StateFlow<List<Store>> = _availableStores.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _saveSuccess = MutableStateFlow<Boolean?>(null)
    val saveSuccess: StateFlow<Boolean?> = _saveSuccess.asStateFlow()

    private val _deleteSuccess = MutableStateFlow<Boolean?>(null)
    val deleteSuccess: StateFlow<Boolean?> = _deleteSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadStores()
    }

    private fun loadStores() {
        viewModelScope.launch {
            repository.getAllStores().collect { stores ->
                _availableStores.value = stores
            }
        }
    }

    fun loadReceipt(receiptId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val receiptEntity = repository.getReceiptById(receiptId)
                _receipt.value = receiptEntity
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error loading receipt"
            } finally {
                _isLoading.value = false
            }
        }

        viewModelScope.launch {
            repository.getInventoryItemsByReceiptId(receiptId).collect { items ->
                _inventoryItems.value = items
            }
        }
    }

    fun saveReceipt(
        dateShopped: LocalDateTime?,
        storeId: Int?,
        storeName: String,
        totalAmount: Float?
    ) {
        val receiptId = _receipt.value?.id ?: return
        
        viewModelScope.launch {
            try {
                var finalStoreId = storeId
                if (storeId == null && storeName.isNotBlank()) {
                    finalStoreId = repository.addStore(storeName)
                }
                repository.updateReceipt(
                    id = receiptId,
                    dateShopped = dateShopped,
                    storeId = finalStoreId,
                    storeName = storeName,
                    totalAmount = totalAmount
                )
                _saveSuccess.value = true
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error saving receipt"
                _saveSuccess.value = false
            }
        }
    }

    fun deleteReceipt() {
        val receiptId = _receipt.value?.id ?: return
        
        viewModelScope.launch {
            try {
                val success = repository.deleteReceipt(receiptId)
                if (success) {
                    _deleteSuccess.value = true
                } else {
                    _deleteSuccess.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error deleting receipt"
                _deleteSuccess.value = false
            }
        }
    }

    fun addStore(name: String) {
        viewModelScope.launch {
            try {
                repository.addStore(name)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error adding store"
            }
        }
    }

    fun clearSaveSuccess() {
        _saveSuccess.value = null
    }

    fun clearDeleteSuccess() {
        _deleteSuccess.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun addMissingItem(productName: String) {
        val receiptId = _receipt.value?.id ?: return
        val trimmedName = productName.trim()
        if (trimmedName.isEmpty()) return

        viewModelScope.launch {
            try {
                val existingProducts = repository.searchProducts(trimmedName).first()
                val productId = if (existingProducts.isNotEmpty()) {
                    existingProducts.first().id
                } else {
                    val newProduct = Product(
                        name = trimmedName,
                        barcode = null,
                        brand = null,
                        category = null
                    )
                    repository.addProductAndGetId(newProduct, mergeByName = false)
                }

                repository.addInventoryItem(
                    productId = productId,
                    dateBought = _receipt.value?.dateShopped,
                    expiryDate = null,
                    storageLocation = null,
                    boughtFromStoreId = _receipt.value?.storeId,
                    nameOverride = null,
                    receiptId = receiptId
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error adding item"
            }
        }
    }
}
