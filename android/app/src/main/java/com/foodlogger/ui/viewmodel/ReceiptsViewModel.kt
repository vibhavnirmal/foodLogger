package com.foodlogger.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foodlogger.data.db.ReceiptEntity
import com.foodlogger.data.repository.FoodLoggerRepository
import com.foodlogger.domain.model.Store
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReceiptsViewModel @Inject constructor(
    private val repository: FoodLoggerRepository
) : ViewModel() {

    private val _receipts = MutableStateFlow<List<ReceiptEntity>>(emptyList())
    val receipts: StateFlow<List<ReceiptEntity>> = _receipts.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _stores = MutableStateFlow<List<Store>>(emptyList())
    val stores: StateFlow<List<Store>> = _stores.asStateFlow()

    init {
        loadReceipts()
        loadStores()
    }

    private fun loadReceipts() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getAllReceipts().collect { receiptList ->
                _receipts.value = receiptList
                _isLoading.value = false
            }
        }
    }

    private fun loadStores() {
        viewModelScope.launch {
            repository.getAllStores().collect { storeList ->
                _stores.value = storeList
            }
        }
    }
}
