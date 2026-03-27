package com.foodlogger.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foodlogger.data.repository.FoodLoggerRepository
import com.foodlogger.domain.model.ReceiptItem
import com.foodlogger.domain.model.Store
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class ReceiptScanViewModel @Inject constructor(
    private val repository: FoodLoggerRepository
) : ViewModel() {

    private val _detectedItems = MutableStateFlow<List<ReceiptItem>>(emptyList())
    val detectedItems: StateFlow<List<ReceiptItem>> = _detectedItems.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _itemsAdded = MutableStateFlow(0)
    val itemsAdded: StateFlow<Int> = _itemsAdded.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _selectedDateShopped = MutableStateFlow(LocalDate.now().atStartOfDay())
    val selectedDateShopped: StateFlow<LocalDateTime> = _selectedDateShopped.asStateFlow()

    private val _selectedStoreId = MutableStateFlow<Int?>(null)
    val selectedStoreId: StateFlow<Int?> = _selectedStoreId.asStateFlow()

    private val _availableStores = MutableStateFlow<List<Store>>(emptyList())
    val availableStores: StateFlow<List<Store>> = _availableStores.asStateFlow()

    private var currentImagePath: String? = null
    private var currentReceiptId: Int? = null

    private val pricedAmountPattern = Regex("""(?<!\d)(\$?\s*(?:\d{1,4}[\.,]\d{2}|[\.,]\d{2}))(?!\d)""")
    private val dollarIntegerPattern = Regex("""\$\s*(\d{1,4})(?!\d)""")

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

    fun setImagePath(path: String) {
        currentImagePath = path
        _selectedDateShopped.value = LocalDate.now().atStartOfDay()
        _selectedStoreId.value = null
    }

    fun setDateShopped(date: LocalDateTime) {
        _selectedDateShopped.value = date.toLocalDate().atStartOfDay()
    }

    fun setStoreId(storeId: Int?) {
        _selectedStoreId.value = storeId
    }

    fun processReceiptText(text: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val lines = text.lines()
                    .map { it.trim() }
                    .filter { it.isNotBlank() && it.length > 2 }
                    .filter { !isHeaderOrFooter(it) }
                    .filter { containsLikelyProductName(it) }

                val parsedNames = lines.map { extractProductName(it) }
                val existingNameMatches = repository.getExistingProductNameMatches(parsedNames)

                val items = lines.mapIndexed { index, line ->
                    val name = extractProductName(line)
                    val price = extractPrice(line)
                    ReceiptItem(
                        id = "receipt_$index",
                        name = name,
                        price = price,
                        quantity = 1,
                        isSelected = true,
                        productExists = existingNameMatches.contains(name.trim().lowercase())
                    )
                }

                _detectedItems.value = items
            } catch (e: Exception) {
                _errorMessage.value = "Error parsing receipt: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private fun isHeaderOrFooter(line: String): Boolean {
        val lowerLine = line.lowercase()
        val ignoreKeywords = listOf(
            "receipt", "total", "subtotal", "tax", "change", "cash", "card", "visa",
            "mastercard", "thank you", "welcome", "receipt #", "transaction",
            "date:", "time:", "store:", "address:", "phone:", "www.", ".com",
            "return", "refund", "credit", "debit", "payment", "balance",
            "member", "savings", "discount", "coupon", "promo"
        )
        return ignoreKeywords.any { lowerLine.contains(it) }
    }

    private fun containsLikelyProductName(line: String): Boolean {
        val hasLetters = line.any { it.isLetter() }
        val hasNumbers = line.any { it.isDigit() }
        val hasDollarSign = line.contains("$")
        val isTooShort = line.length < 3
        val isAllNumbers = line.all { it.isDigit() || it.isWhitespace() || it == '.' }

        return hasLetters && !isTooShort && !isAllNumbers &&
                (hasNumbers || hasDollarSign || line.split(" ").size >= 2)
    }

    private fun extractProductName(line: String): String {
        val parts = line.split(Regex("""\s{2,}"""))
        if (parts.size > 1) {
            return parts.first().trim()
        }

        val quantityPriceSuffixRemoved = line
            .replace(Regex("""\s+\d+\s*[@xX]\s*\$?\s*(?:\d{1,4}[\.,]\d{2}|[\.,]\d{2})\s*$"""), "")
            .trim()
        val priceSuffixRemoved = quantityPriceSuffixRemoved
            .replace(Regex("""\s+\$?\s*(?:\d{1,4}[\.,]\d{2}|[\.,]\d{2})\s*$"""), "")
            .trim()

        return priceSuffixRemoved.ifBlank { line }
    }

    private fun extractPrice(line: String): Float? {
        val normalized = normalizeLineForPrice(line)

        val decimalCandidates = pricedAmountPattern.findAll(normalized)
            .mapNotNull { parsePriceToken(it.groupValues[1]) }
            .filter { it in 0.01f..9999f }
            .toList()
        if (decimalCandidates.isNotEmpty()) {
            return decimalCandidates.last()
        }

        val dollarIntegerCandidates = dollarIntegerPattern.findAll(normalized)
            .mapNotNull { parsePriceToken(it.groupValues[1]) }
            .filter { it in 0.01f..9999f }
            .toList()
        if (dollarIntegerCandidates.isNotEmpty()) {
            return dollarIntegerCandidates.last()
        }

        return null
    }

    private fun normalizeLineForPrice(line: String): String {
        // Common OCR correction: O between digits is usually 0 in prices.
        return line.replace(Regex("""(?<=\d)[oO](?=\d)"""), "0")
    }

    private fun parsePriceToken(rawToken: String): Float? {
        var token = rawToken
            .replace("$", "")
            .replace(" ", "")

        if (token.isBlank()) {
            return null
        }

        token = when {
            token.contains(',') && token.contains('.') -> {
                if (token.lastIndexOf('.') > token.lastIndexOf(',')) {
                    token.replace(",", "")
                } else {
                    token.replace(".", "").replace(',', '.')
                }
            }
            token.contains(',') -> token.replace(',', '.')
            else -> token
        }

        if (token.startsWith('.')) {
            token = "0$token"
        }

        return token.toFloatOrNull()
    }

    fun toggleItemSelection(itemId: String, isSelected: Boolean) {
        _detectedItems.value = _detectedItems.value.map { item ->
            if (item.id == itemId) item.copy(isSelected = isSelected) else item
        }
    }

    suspend fun addSelectedItemsToInventory(): Int {
        val dateShopped = _selectedDateShopped.value
        val storeId = _selectedStoreId.value

        // Check for duplicate if both date and store are set
        if (dateShopped != null && storeId != null) {
            val existingReceipt = repository.findDuplicateReceipt(dateShopped, storeId)
            if (existingReceipt != null) {
                _errorMessage.value = "A receipt for this date and location already exists"
                return 0
            }
        }

        _isProcessing.value = true
        var addedCount = 0
        try {
            val imagePath = currentImagePath
            if (imagePath != null) {
                currentReceiptId = repository.saveReceipt(
                    imagePath = imagePath,
                    dateShopped = dateShopped,
                    storeId = storeId
                )
            }

            val receiptId = currentReceiptId
            val selectedItems = _detectedItems.value.filter { it.isSelected }
            val itemNames = selectedItems.map { it.name }

            if (receiptId != null && itemNames.isNotEmpty()) {
                addedCount = repository.addItemsFromReceipt(receiptId, itemNames)
            }
            _itemsAdded.value = addedCount
            return addedCount
        } catch (e: Exception) {
            _errorMessage.value = "Error adding items: ${e.message}"
            return 0
        } finally {
            _isProcessing.value = false
        }
    }

    fun clearItems() {
        _detectedItems.value = emptyList()
        currentImagePath = null
        currentReceiptId = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun setProcessing(processing: Boolean) {
        _isProcessing.value = processing
    }
}
