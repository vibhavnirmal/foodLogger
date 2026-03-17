package com.foodlogger.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.foodlogger.ui.components.CameraBarcodeScannerView
import com.foodlogger.ui.components.DatePickerField
import com.foodlogger.ui.viewmodel.BarcodeViewModel
import kotlinx.coroutines.launch

@Composable
fun BarcodeScreen(viewModel: BarcodeViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val scannedBarcode by viewModel.scannedBarcode.collectAsState()
    val product by viewModel.product.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val expiryDateInput by viewModel.expiryDateInput.collectAsState()
    val dateBoughtInput by viewModel.dateBoughtInput.collectAsState()
    val storageLocation by viewModel.storageLocation.collectAsState()
    val nameOverride by viewModel.nameOverride.collectAsState()

    var tabIndex by remember { mutableStateOf(0) }
    var barcodeInput by remember { mutableStateOf("") }
    var quantityInput by remember { mutableStateOf("1") }
    var manualProductName by remember { mutableStateOf("") }
    var manualProductBrand by remember { mutableStateOf("") }
    var cameraPermissionGranted by remember {
        mutableStateOf(context.hasCameraPermission())
    }
    var cameraPermissionRequested by remember { mutableStateOf(false) }
    var validationMode by remember { mutableStateOf<BarcodeValidationMode?>(null) }

    val barcodeError = if (validationMode != null && barcodeInput.isBlank()) "Barcode is required" else null
    val quantityError = if (
        validationMode == BarcodeValidationMode.ADD_FOUND || validationMode == BarcodeValidationMode.ADD_MANUAL
    ) {
        if (quantityInput.toPositiveFloatOrNull() == null) "Enter a quantity greater than 0" else null
    } else {
        null
    }
    val manualProductNameError = if (
        validationMode == BarcodeValidationMode.ADD_MANUAL && manualProductName.isBlank()
    ) {
        "Product name is required"
    } else {
        null
    }

    val coroutineScope = rememberCoroutineScope()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionGranted = granted
        cameraPermissionRequested = true
    }

    LaunchedEffect(tabIndex) {
        if (tabIndex == 0 && !cameraPermissionGranted) {
            cameraPermissionRequested = true
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Barcode Scanner",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                // Tab selection
                TabRow(selectedTabIndex = tabIndex) {
                    Tab(
                        selected = tabIndex == 0,
                        onClick = { tabIndex = 0 },
                        text = { Text("Camera") }
                    )
                    Tab(
                        selected = tabIndex == 1,
                        onClick = { tabIndex = 1 },
                        text = { Text("Manual Entry") }
                    )
                }
            }
        }
    ) { paddingValues ->
        when (tabIndex) {
            0 -> {
                val shouldShowRationale = context.findActivity()?.let { activity ->
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        Manifest.permission.CAMERA
                    )
                } ?: false

                if (cameraPermissionGranted) {
                    CameraScannerTab(
                        modifier = Modifier.padding(paddingValues),
                        onBarcodeDetected = { barcode ->
                            barcodeInput = barcode
                            viewModel.setManualBarcode(barcode)
                            tabIndex = 1
                        },
                        onCameraError = {
                            tabIndex = 1
                        }
                    )
                } else {
                    CameraPermissionTab(
                        modifier = Modifier.padding(paddingValues),
                        showRationale = shouldShowRationale,
                        permissionRequested = cameraPermissionRequested,
                        onGrantPermission = {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        onUseManualEntry = {
                            tabIndex = 1
                        }
                    )
                }
            }
            1 -> {
                // Manual Entry Mode
                ManualEntryTab(
                    modifier = Modifier.padding(paddingValues),
                    scannedBarcode = scannedBarcode,
                    product = product,
                    errorMessage = errorMessage,
                    isLoading = isLoading,
                    barcodeInput = barcodeInput,
                    quantityInput = quantityInput,
                    manualProductName = manualProductName,
                    manualProductBrand = manualProductBrand,
                    expiryDateInput = expiryDateInput,
                    dateBoughtInput = dateBoughtInput,
                    storageLocation = storageLocation,
                    nameOverride = nameOverride,
                    barcodeError = barcodeError,
                    quantityError = quantityError,
                    manualProductNameError = manualProductNameError,
                    onBarcodeInputChange = { barcodeInput = it },
                    onQuantityInputChange = { quantityInput = it },
                    onProductNameChange = { manualProductName = it },
                    onProductBrandChange = { manualProductBrand = it },
                    onExpiryDateInputChange = viewModel::setExpiryDateInput,
                    onDateBoughtInputChange = viewModel::setDateBoughtInput,
                    onStorageLocationChange = viewModel::setStorageLocation,
                    onNameOverrideChange = viewModel::setNameOverride,
                    onSearch = {
                        validationMode = BarcodeValidationMode.SEARCH
                        if (barcodeInput.isBlank()) {
                            return@ManualEntryTab
                        }
                        viewModel.setManualBarcode(barcodeInput)
                    },
                    onAddToInventory = {
                        coroutineScope.launch {
                            validationMode = BarcodeValidationMode.ADD_FOUND
                            val qty = quantityInput.toPositiveFloatOrNull() ?: return@launch
                            viewModel.setQuantity(qty)
                            viewModel.addScannedItemToInventory()
                            barcodeInput = ""
                            quantityInput = "1"
                            manualProductName = ""
                            manualProductBrand = ""
                            validationMode = null
                        }
                    },
                    onAddProductAndInventory = {
                        coroutineScope.launch {
                            validationMode = BarcodeValidationMode.ADD_MANUAL
                            val barcode = scannedBarcode ?: return@launch
                            val qty = quantityInput.toPositiveFloatOrNull() ?: return@launch
                            val manualName = manualProductName.trim().takeIf { it.isNotEmpty() } ?: return@launch
                            viewModel.setQuantity(qty)
                            viewModel.addManualProductAndInventory(
                                barcode = barcode,
                                name = manualName,
                                brand = manualProductBrand.takeIf { it.isNotEmpty() }
                            )
                            barcodeInput = ""
                            quantityInput = "1"
                            manualProductName = ""
                            manualProductBrand = ""
                            validationMode = null
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CameraPermissionTab(
    modifier: Modifier = Modifier,
    showRationale: Boolean,
    permissionRequested: Boolean,
    onGrantPermission: () -> Unit,
    onUseManualEntry: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Camera access is required to scan barcodes.",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = when {
                        showRationale -> "Allow camera access to open the live scanner."
                        permissionRequested -> "Permission is currently denied. Grant camera access to continue, or use manual entry."
                        else -> "Tap below to allow camera access and start scanning."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onGrantPermission,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Camera Permission")
                }
                Button(
                    onClick = onUseManualEntry,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Use Manual Entry")
                }
            }
        }
    }
}

@Composable
private fun CameraScannerTab(
    modifier: Modifier = Modifier,
    onBarcodeDetected: (String) -> Unit,
    onCameraError: (String) -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        CameraBarcodeScannerView(
            onBarcodeDetected = onBarcodeDetected,
            onCameraError = onCameraError
        )
    }
}

private fun Context.hasCameraPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}

private fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

@Composable
private fun ManualEntryTab(
    modifier: Modifier = Modifier,
    scannedBarcode: String?,
    product: com.foodlogger.domain.model.Product?,
    errorMessage: String?,
    isLoading: Boolean,
    barcodeInput: String,
    quantityInput: String,
    manualProductName: String,
    manualProductBrand: String,
    expiryDateInput: String,
    dateBoughtInput: String,
    storageLocation: String,
    nameOverride: String,
    barcodeError: String?,
    quantityError: String?,
    manualProductNameError: String?,
    onBarcodeInputChange: (String) -> Unit,
    onQuantityInputChange: (String) -> Unit,
    onProductNameChange: (String) -> Unit,
    onProductBrandChange: (String) -> Unit,
    onExpiryDateInputChange: (String) -> Unit,
    onDateBoughtInputChange: (String) -> Unit,
    onStorageLocationChange: (String) -> Unit,
    onNameOverrideChange: (String) -> Unit,
    onSearch: () -> Unit,
    onAddToInventory: () -> Unit,
    onAddProductAndInventory: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Barcode Input
        OutlinedTextField(
            value = barcodeInput,
            onValueChange = onBarcodeInputChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter barcode...") },
            label = { Text("Barcode") },
            isError = barcodeError != null,
            supportingText = barcodeError?.let { message -> { Text(message) } },
            singleLine = true
        )

        Button(
            onClick = onSearch,
            modifier = Modifier.fillMaxWidth(),
            enabled = barcodeInput.isNotEmpty() && !isLoading
        ) {
            Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text("Search Product")
        }

        // Error Message
        if (errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFEBEE)
                )
            ) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFC62828),
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Product Info (if found)
        if (product != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Inventory Details",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "✓ Product Found: ${product.name}",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF66BB6A)
                    )
                    if (!product.brand.isNullOrEmpty()) {
                        Text(
                            text = "Brand: ${product.brand}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!product.category.isNullOrEmpty()) {
                        Text(
                            text = "Category: ${product.category}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Quantity Input
                    OutlinedTextField(
                        value = quantityInput,
                        onValueChange = onQuantityInputChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Quantity") },
                        isError = quantityError != null,
                        supportingText = quantityError?.let { message -> { Text(message) } },
                        singleLine = true
                    )

                    InventoryContextFields(
                        expiryDateInput = expiryDateInput,
                        dateBoughtInput = dateBoughtInput,
                        storageLocation = storageLocation,
                        nameOverride = nameOverride,
                        onExpiryDateInputChange = onExpiryDateInputChange,
                        onDateBoughtInputChange = onDateBoughtInputChange,
                        onStorageLocationChange = onStorageLocationChange,
                        onNameOverrideChange = onNameOverrideChange,
                    )

                    // Add to Inventory Button
                    Button(
                        onClick = onAddToInventory,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add to Inventory")
                    }
                }
            }
        }

        // Manual Product Entry (if product not found)
        if (scannedBarcode != null && product == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Add Product Manually",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Product Details",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = manualProductName,
                        onValueChange = onProductNameChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Product Name") },
                        isError = manualProductNameError != null,
                        supportingText = manualProductNameError?.let { message -> { Text(message) } },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = manualProductBrand,
                        onValueChange = onProductBrandChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Brand (Optional)") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = quantityInput,
                        onValueChange = onQuantityInputChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Quantity") },
                        isError = quantityError != null,
                        supportingText = quantityError?.let { message -> { Text(message) } },
                        singleLine = true
                    )

                    Text(
                        text = "Inventory Details",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    InventoryContextFields(
                        expiryDateInput = expiryDateInput,
                        dateBoughtInput = dateBoughtInput,
                        storageLocation = storageLocation,
                        nameOverride = nameOverride,
                        onExpiryDateInputChange = onExpiryDateInputChange,
                        onDateBoughtInputChange = onDateBoughtInputChange,
                        onStorageLocationChange = onStorageLocationChange,
                        onNameOverrideChange = onNameOverrideChange,
                    )

                    Button(
                        onClick = onAddProductAndInventory,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = manualProductName.isNotEmpty()
                    ) {
                        Text("Add Product and Inventory")
                    }
                }
            }
        }
    }
}

private enum class BarcodeValidationMode {
    SEARCH,
    ADD_FOUND,
    ADD_MANUAL,
}

private fun String.toPositiveFloatOrNull(): Float? = toFloatOrNull()?.takeIf { it > 0f }

@Composable
private fun InventoryContextFields(
    expiryDateInput: String,
    dateBoughtInput: String,
    storageLocation: String,
    nameOverride: String,
    onExpiryDateInputChange: (String) -> Unit,
    onDateBoughtInputChange: (String) -> Unit,
    onStorageLocationChange: (String) -> Unit,
    onNameOverrideChange: (String) -> Unit,
) {
    DatePickerField(
        label = "Expiry Date",
        value = expiryDateInput,
        onValueChange = onExpiryDateInputChange,
    )

    DatePickerField(
        label = "Date Bought",
        value = dateBoughtInput,
        onValueChange = onDateBoughtInputChange,
    )

    OutlinedTextField(
        value = storageLocation,
        onValueChange = onStorageLocationChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Storage Location") },
        singleLine = true
    )

    OutlinedTextField(
        value = nameOverride,
        onValueChange = onNameOverrideChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Display Name Override") },
        singleLine = true
    )
}
