package com.foodlogger.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foodlogger.data.repository.FoodLoggerRepository
import com.foodlogger.domain.model.Product
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
class BarcodeViewModel @Inject constructor(
    private val repository: FoodLoggerRepository
) : ViewModel() {

    private val _scannedBarcode = MutableStateFlow<String?>(null)
    val scannedBarcode: StateFlow<String?> = _scannedBarcode.asStateFlow()

    private val _product = MutableStateFlow<Product?>(null)
    val product: StateFlow<Product?> = _product.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _quantity = MutableStateFlow(1.0f)
    val quantity: StateFlow<Float> = _quantity.asStateFlow()

    private val _expiryDate = MutableStateFlow<LocalDateTime?>(null)
    val expiryDate: StateFlow<LocalDateTime?> = _expiryDate.asStateFlow()

    private val _expiryDateInput = MutableStateFlow("")
    val expiryDateInput: StateFlow<String> = _expiryDateInput.asStateFlow()

    private val _dateBought = MutableStateFlow<LocalDateTime?>(null)
    val dateBought: StateFlow<LocalDateTime?> = _dateBought.asStateFlow()

    private val _dateBoughtInput = MutableStateFlow("")
    val dateBoughtInput: StateFlow<String> = _dateBoughtInput.asStateFlow()

    private val _storageLocation = MutableStateFlow("")
    val storageLocation: StateFlow<String> = _storageLocation.asStateFlow()

    private val _availableStorageLocations = MutableStateFlow<List<String>>(emptyList())
    val availableStorageLocations: StateFlow<List<String>> = _availableStorageLocations.asStateFlow()

    private val _availableStores = MutableStateFlow<List<Store>>(emptyList())
    val availableStores: StateFlow<List<Store>> = _availableStores.asStateFlow()

    private val _boughtFromStoreId = MutableStateFlow<Int?>(null)
    val boughtFromStoreId: StateFlow<Int?> = _boughtFromStoreId.asStateFlow()

    private val _nameOverride = MutableStateFlow("")
    val nameOverride: StateFlow<String> = _nameOverride.asStateFlow()

    init {
        observeStorageLocations()
        observeStores()
    }

    private fun observeStorageLocations() {
        viewModelScope.launch {
            runCatching {
                repository.getAllStorageLocations().collect { locations ->
                    _availableStorageLocations.value = locations.map { it.name }
                }
            }.onFailure {
                _errorMessage.value = it.message ?: "Error loading storage locations"
            }
        }
    }

    private fun observeStores() {
        viewModelScope.launch {
            runCatching {
                repository.getAllStores().collect { stores ->
                    _availableStores.value = stores
                }
            }.onFailure {
                _errorMessage.value = it.message ?: "Error loading stores"
            }
        }
    }

    fun setBarcodeScanned(barcode: String) {
        _errorMessage.value = null
        _product.value = null

        if (!isValidBarcode(barcode)) {
            _errorMessage.value = "Invalid barcode format"
            return
        }

        _scannedBarcode.value = barcode
        lookupProduct(barcode)
    }

    fun setManualBarcode(barcode: String) {
        setBarcodeScanned(barcode)
    }

    private fun lookupProduct(barcode: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val foundProduct = repository.getProduct(barcode)
                _product.value = foundProduct
                if (foundProduct == null) {
                    _errorMessage.value = "Product not found. Add manually or use Open Food Facts."
                } else {
                    _errorMessage.value = null
                }
            } catch (e: Exception) {
                _product.value = null
                _errorMessage.value = e.message ?: "Error looking up product"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setQuantity(q: Float) {
        _quantity.value = q
    }

    fun setExpiryDate(date: LocalDateTime?) {
        _expiryDate.value = date
    }

    fun setExpiryDateInput(value: String) {
        _expiryDateInput.value = value
        _expiryDate.value = parseDate(value)
    }

    fun setDateBoughtInput(value: String) {
        _dateBoughtInput.value = value
        _dateBought.value = parseDate(value)
    }

    fun setStorageLocation(value: String) {
        _storageLocation.value = value
    }

    fun setNameOverride(value: String) {
        _nameOverride.value = value
    }

    fun setBoughtFromStoreId(value: Int?) {
        _boughtFromStoreId.value = value
    }

    suspend fun addScannedItemToInventory() {
        if (_scannedBarcode.value == null) {
            _errorMessage.value = "No barcode scanned"
            return
        }

        val product = _product.value
        if (product == null) {
            _errorMessage.value = "No product found. Add manually first."
            return
        }

        try {
            repository.addInventoryItem(
                productId = product.id,
                quantity = _quantity.value,
                unit = "unit",
                dateBought = _dateBought.value ?: LocalDateTime.now(),
                expiryDate = _expiryDate.value,
                storageLocation = _storageLocation.value.trim().ifEmpty { null },
                boughtFromStoreId = _boughtFromStoreId.value,
                nameOverride = _nameOverride.value.trim().ifEmpty { null }
            )
            // Reset form
            resetForm()
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Error adding to inventory"
        }
    }

    suspend fun addManualProductAndInventory(
        barcode: String,
        name: String,
        brand: String? = null
    ) {
        try {
            val product = Product(
                barcode = barcode,
                name = name,
                brand = brand,
                category = null,
                servingSize = null,
                kcal = null,
                protein = null,
                carbs = null,
                fat = null
            )
            repository.addProductWithInventory(
                product = product,
                quantity = _quantity.value,
                unit = "unit",
                dateBought = _dateBought.value ?: LocalDateTime.now(),
                expiryDate = _expiryDate.value,
                storageLocation = _storageLocation.value.trim().ifEmpty { null },
                boughtFromStoreId = _boughtFromStoreId.value,
                nameOverride = _nameOverride.value.trim().ifEmpty { null }
            )
            resetForm()
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Error adding product and inventory"
        }
    }

    suspend fun addManualProduct(
        barcode: String,
        name: String,
        brand: String? = null
    ) {
        try {
            repository.addProduct(
                Product(
                    barcode = barcode,
                    name = name,
                    brand = brand,
                    category = null,
                    servingSize = null,
                    kcal = null,
                    protein = null,
                    carbs = null,
                    fat = null
                )
            )
            _product.value = Product(
                barcode = barcode,
                name = name,
                brand = brand,
                category = null,
                servingSize = null,
                kcal = null,
                protein = null,
                carbs = null,
                fat = null
            )
            _errorMessage.value = null
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Error adding product"
        }
    }

    private fun resetForm() {
        _scannedBarcode.value = null
        _product.value = null
        _quantity.value = 1.0f
        _expiryDate.value = null
        _expiryDateInput.value = ""
        _dateBought.value = null
        _dateBoughtInput.value = ""
        _storageLocation.value = ""
        _boughtFromStoreId.value = null
        _nameOverride.value = ""
        _errorMessage.value = null
    }

    private fun parseDate(value: String): LocalDateTime? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) {
            return null
        }

        return runCatching {
            LocalDate.parse(trimmed).atStartOfDay()
        }.getOrNull()
    }

    private fun isValidBarcode(barcode: String): Boolean {
        // Barcode validation logic from web app (barcode.js)
        if (!barcode.all { it.isDigit() }) return false
        val length = barcode.length
        if (length !in setOf(8, 12, 13, 14)) return false
        
        // Mod-10 checksum validation
        return validateMod10Checksum(barcode)
    }

    private fun validateMod10Checksum(barcode: String): Boolean {
        if (barcode.length < 2) return false
        
        val digits = barcode.dropLast(1).map { it.digitToInt() }
        var sum = 0
        for ((index, digit) in digits.withIndex()) {
            val multiplier = if ((digits.size - index) % 2 == 0) 1 else 3
            sum += digit * multiplier
        }
        
        val checkDigit = (10 - (sum % 10)) % 10
        return checkDigit == barcode.last().digitToInt()
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
