package com.foodlogger.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foodlogger.data.repository.FoodLoggerRepository
import com.foodlogger.domain.model.Category
import com.foodlogger.domain.model.Store
import com.foodlogger.domain.model.StorageLocation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: FoodLoggerRepository
) : ViewModel() {

    private val _locations = MutableStateFlow<List<StorageLocation>>(emptyList())
    val locations: StateFlow<List<StorageLocation>> = _locations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _stores = MutableStateFlow<List<Store>>(emptyList())
    val stores: StateFlow<List<Store>> = _stores.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        observeLocations()
        observeStores()
        observeCategories()
    }

    private fun observeLocations() {
        viewModelScope.launch {
            _isLoading.value = true
            var firstEmission = true
            try {
                repository.getAllStorageLocations().collect { items ->
                    _locations.value = items
                    if (firstEmission) {
                        _isLoading.value = false
                        firstEmission = false
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load storage locations"
                _isLoading.value = false
            }
        }
    }

    private fun observeStores() {
        viewModelScope.launch {
            try {
                repository.getAllStores().collect { items ->
                    _stores.value = items
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load stores"
            }
        }
    }

    private fun observeCategories() {
        viewModelScope.launch {
            try {
                repository.getAllCategories().collect { items ->
                    _categories.value = items
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load categories"
            }
        }
    }

    fun addLocation(name: String) {
        viewModelScope.launch {
            runCatching { repository.addStorageLocation(name) }
                .onFailure { _errorMessage.value = it.message ?: "Failed to add storage location" }
        }
    }

    fun renameLocation(id: Int, newName: String) {
        viewModelScope.launch {
            runCatching { repository.renameStorageLocation(id, newName) }
                .onFailure { _errorMessage.value = it.message ?: "Failed to rename storage location" }
        }
    }

    fun deleteLocation(id: Int) {
        viewModelScope.launch {
            runCatching { repository.deleteStorageLocation(id) }
                .onFailure { _errorMessage.value = it.message ?: "Failed to delete storage location" }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun addStore(name: String, imageUri: String?) {
        viewModelScope.launch {
            runCatching { repository.addStore(name, imageUri) }
                .onFailure { _errorMessage.value = it.message ?: "Failed to add store" }
        }
    }

    fun renameStore(id: Int, newName: String) {
        viewModelScope.launch {
            runCatching { repository.renameStore(id, newName) }
                .onFailure { _errorMessage.value = it.message ?: "Failed to rename store" }
        }
    }

    fun updateStoreImage(id: Int, imageUri: String?) {
        viewModelScope.launch {
            runCatching { repository.updateStoreImage(id, imageUri) }
                .onFailure { _errorMessage.value = it.message ?: "Failed to update store image" }
        }
    }

    fun deleteStore(id: Int) {
        viewModelScope.launch {
            runCatching { repository.deleteStore(id) }
                .onFailure { _errorMessage.value = it.message ?: "Failed to delete store" }
        }
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            runCatching { repository.addCategory(name) }
                .onFailure { _errorMessage.value = it.message ?: "Failed to add category" }
        }
    }

    fun renameCategory(id: Int, newName: String) {
        viewModelScope.launch {
            runCatching { repository.renameCategory(id, newName) }
                .onFailure { _errorMessage.value = it.message ?: "Failed to rename category" }
        }
    }

    fun deleteCategory(id: Int) {
        viewModelScope.launch {
            runCatching { repository.deleteCategory(id) }
                .onFailure { _errorMessage.value = it.message ?: "Failed to delete category" }
        }
    }
}
