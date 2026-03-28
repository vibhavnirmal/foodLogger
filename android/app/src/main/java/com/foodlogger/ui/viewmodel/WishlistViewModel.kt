package com.foodlogger.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foodlogger.data.repository.FoodLoggerRepository
import com.foodlogger.domain.model.InventoryItem
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WishlistViewModel @Inject constructor(
    private val repository: FoodLoggerRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val preferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val _almostFinishedItems = MutableStateFlow<List<InventoryItem>>(emptyList())
    val almostFinishedItems: StateFlow<List<InventoryItem>> = _almostFinishedItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _checkedItemIds = MutableStateFlow<Set<Int>>(emptySet())
    val checkedItemIds: StateFlow<Set<Int>> = _checkedItemIds.asStateFlow()

    init {
        _checkedItemIds.value = loadPersistedCheckedIds()
        loadAlmostFinishedItems()
    }

    private fun loadAlmostFinishedItems() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            var firstEmission = true
            try {
                repository.getAlmostFinishedItems().collect { items ->
                    _almostFinishedItems.value = items
                    val validCheckedIds = _checkedItemIds.value.intersect(items.map { it.id }.toSet())
                    if (validCheckedIds != _checkedItemIds.value) {
                        _checkedItemIds.value = validCheckedIds
                        persistCheckedItemIds(validCheckedIds)
                    }
                    _errorMessage.value = null
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

    fun toggleItemChecked(id: Int) {
        val updatedIds = if (_checkedItemIds.value.contains(id)) {
            _checkedItemIds.value - id
        } else {
            _checkedItemIds.value + id
        }
        _checkedItemIds.value = updatedIds
        persistCheckedItemIds(updatedIds)
    }

    fun clearShoppedItems() {
        viewModelScope.launch {
            try {
                val checkedIds = _checkedItemIds.value
                val itemsById = _almostFinishedItems.value.associateBy { it.id }
                checkedIds.forEach { id ->
                    val item = itemsById[id] ?: return@forEach
                    repository.updateInventoryItem(
                        id = id,
                        expiryDate = item.expiryDate,
                        storageLocation = item.storageLocation,
                        boughtFromStoreId = item.boughtFromStoreId,
                        nameOverride = item.nameOverride,
                        almostFinished = false
                    )
                }
                _checkedItemIds.value = emptySet()
                persistCheckedItemIds(emptySet())
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error clearing shopped items"
            }
        }
    }

    private fun loadPersistedCheckedIds(): Set<Int> {
        return preferences
            .getStringSet(KEY_CHECKED_ITEM_IDS, emptySet())
            .orEmpty()
            .mapNotNull { it.toIntOrNull() }
            .toSet()
    }

    private fun persistCheckedItemIds(ids: Set<Int>) {
        preferences.edit()
            .putStringSet(KEY_CHECKED_ITEM_IDS, ids.map { it.toString() }.toSet())
            .apply()
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

    companion object {
        private const val PREFS_NAME = "wishlist_prefs"
        private const val KEY_CHECKED_ITEM_IDS = "checked_item_ids"
    }
}
