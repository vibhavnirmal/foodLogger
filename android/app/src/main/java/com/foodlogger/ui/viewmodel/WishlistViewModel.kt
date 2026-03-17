package com.foodlogger.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foodlogger.data.repository.FoodLoggerRepository
import com.foodlogger.domain.model.InventoryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WishlistViewModel @Inject constructor(
    private val repository: FoodLoggerRepository
) : ViewModel() {

    private val _almostFinishedItems = MutableStateFlow<List<InventoryItem>>(emptyList())
    val almostFinishedItems: StateFlow<List<InventoryItem>> = _almostFinishedItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadAlmostFinishedItems()
    }

    private fun loadAlmostFinishedItems() {
        viewModelScope.launch {
            _isLoading.value = true
            var firstEmission = true
            try {
                repository.getAlmostFinishedItems().collect { items ->
                    _almostFinishedItems.value = items
                    if (firstEmission) {
                        _isLoading.value = false
                        firstEmission = false
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Unknown error loading wishlist"
                _isLoading.value = false
            }
        }
    }

    suspend fun unmarkAlmostFinished(id: Int) {
        try {
            val item = _almostFinishedItems.value.find { it.id == id } ?: return
            repository.updateInventoryItem(
                id = id,
                quantity = item.quantity,
                expiryDate = item.expiryDate,
                storageLocation = item.storageLocation,
                boughtFromStoreId = item.boughtFromStoreId,
                nameOverride = item.nameOverride,
                almostFinished = false
            )
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Error updating item"
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // Public convenience methods for UI
    fun markAsFinished(id: Int) {
        viewModelScope.launch {
            unmarkAlmostFinished(id)
        }
    }

    suspend fun deleteItem(id: Int) {
        try {
            repository.deleteInventoryItem(id)
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Error deleting item"
        }
    }

    fun reloadWishlist() {
        loadAlmostFinishedItems()
    }
}
