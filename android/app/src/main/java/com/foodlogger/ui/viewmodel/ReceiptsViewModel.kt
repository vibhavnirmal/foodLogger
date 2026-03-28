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

enum class ReceiptSortOption {
    DATE_SHOPPED_DESC,
    DATE_SHOPPED_ASC,
    AMOUNT_DESC,
    AMOUNT_ASC
}

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

    private val _itemCounts = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val itemCounts: StateFlow<Map<Int, Int>> = _itemCounts.asStateFlow()

    private val _totalAmount = MutableStateFlow(0f)
    val totalAmount: StateFlow<Float> = _totalAmount.asStateFlow()

    private var sortOption = ReceiptSortOption.DATE_SHOPPED_DESC
    private var filterStoreId: Int? = null
    private var allReceipts: List<ReceiptEntity> = emptyList()

    init {
        loadReceipts()
        loadStores()
        loadItemCounts()
    }

    private fun loadReceipts() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getAllReceipts().collect { receiptList ->
                allReceipts = receiptList
                applySortAndFilter()
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

    private fun loadItemCounts() {
        viewModelScope.launch {
            repository.getAllInventory().collect { inventory ->
                val counts = inventory
                    .filter { it.receiptId != null }
                    .groupBy { it.receiptId!! }
                    .mapValues { it.value.size }
                _itemCounts.value = counts
            }
        }
    }

    fun setSortOption(option: ReceiptSortOption) {
        sortOption = option
        applySortAndFilter()
    }

    fun setFilterStoreId(storeId: Int?) {
        filterStoreId = storeId
        applySortAndFilter()
    }

    private fun applySortAndFilter() {
        var result = allReceipts

        if (filterStoreId != null) {
            result = result.filter { it.storeId == filterStoreId }
        }

        _totalAmount.value = result.sumOf { (it.totalAmount ?: 0f).toDouble() }.toFloat()

        result = when (sortOption) {
            ReceiptSortOption.DATE_SHOPPED_DESC -> result.sortedByDescending { it.dateShopped ?: it.dateScanned }
            ReceiptSortOption.DATE_SHOPPED_ASC -> result.sortedBy { it.dateShopped ?: it.dateScanned }
            ReceiptSortOption.AMOUNT_DESC -> result.sortedByDescending { it.totalAmount ?: 0f }
            ReceiptSortOption.AMOUNT_ASC -> result.sortedBy { it.totalAmount ?: 0f }
        }

        _receipts.value = result
    }
}
