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

    private val pricePattern = Regex(
        """(?:\$|€|£|₹|USD|EUR|GBP|INR)?\s*\d{1,3}(?:[\s,.]\d{3})*(?:[.,]\d{2})?""",
        RegexOption.IGNORE_CASE
    )

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

                val items = lines.mapIndexed { index, line ->
                    val name = extractProductName(line)
                    val price = extractPrice(line)
                    ReceiptItem(
                        id = "receipt_$index",
                        name = name,
                        price = price,
                        quantity = 1,
                        isSelected = true
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

        val priceRemoved = line.replace(Regex("""\$?\d+\.?\d{0,2}\s*$"""), "").trim()
        return priceRemoved.ifBlank { line }
    }

    private fun extractPrice(line: String): Float? {
        // OCR often inserts spaces around separators (e.g. "1 . 99"), so compact those first.
        val compacted = line
            .replace(Regex("""(\d)\s*([.,])\s*(\d)"""), "$1$2$3")
            .replace(Regex("""([€£₹$])\s+(\d)"""), "$1$2")

        val candidates = pricePattern.findAll(compacted)
            .mapNotNull { normalizePriceToken(it.value) }
            .toList()

        if (candidates.isNotEmpty()) {
            // In most receipts the item price appears at the end of the line.
            return candidates.last()
        }

        // Fallback for OCR like "199" intended as "1.99" at end of the line.
        val trailingCents = Regex("""(\d+)\s?(\d{2})$""").find(compacted)
        return trailingCents?.let {
            "${it.groupValues[1]}.${it.groupValues[2]}".toFloatOrNull()
        }
    }

    private fun normalizePriceToken(raw: String): Float? {
        var token = raw.trim()
            .replace(Regex("""(?i)USD|EUR|GBP|INR"""), "")
            .replace(Regex("""[€£₹$]"""), "")
            .replace(" ", "")

        if (token.isBlank()) return null

        val lastDot = token.lastIndexOf('.')
        val lastComma = token.lastIndexOf(',')

        token = when {
            lastDot >= 0 && lastComma >= 0 -> {
                // Choose the rightmost separator as decimal separator.
                if (lastDot > lastComma) {
                    token.replace(",", "")
                } else {
                    token.replace(".", "").replace(',', '.')
                }
            }
            lastComma >= 0 -> {
                val decimals = token.length - lastComma - 1
                if (decimals == 2) token.replace(',', '.') else token.replace(",", "")
            }
            else -> token
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
