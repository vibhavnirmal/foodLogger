package com.foodlogger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.foodlogger.domain.model.ExpiryStatus
import com.foodlogger.domain.model.InventoryItem
import com.foodlogger.domain.model.Product
import com.foodlogger.ui.components.DatePickerField
import com.foodlogger.ui.components.InventoryListItem
import com.foodlogger.ui.components.SearchableProductSelector
import com.foodlogger.ui.viewmodel.InventoryViewModel
import java.time.LocalDate

@Composable
fun InventoryScreen(viewModel: InventoryViewModel = hiltViewModel()) {
    val filteredItems by viewModel.filteredItems.collectAsState()
    val availableProducts by viewModel.availableProducts.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val filterByStatus by viewModel.filterByStatus.collectAsState()
    var editingItem by remember { mutableStateOf<InventoryItem?>(null) }
    var creatingItem by remember { mutableStateOf(false) }

    var sortExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Inventory",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Sort Dropdown
                Button(
                    onClick = { sortExpanded = !sortExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.FilterList, contentDescription = null)
                    Text("Sort: ${sortBy.name.replace("_", " ")}")
                }

                if (sortExpanded) {
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)) {
                        InventoryViewModel.SortOption.entries.forEach { option ->
                            Text(
                                text = option.name.replace("_", " "),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.sortItemsBy(option)
                                        sortExpanded = false
                                    }
                                    .padding(12.dp)
                            )
                        }
                    }
                }

                // Status Filter
                Button(
                    onClick = { statusExpanded = !statusExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Status: ${filterByStatus?.name ?: "ALL"}")
                }

                Button(
                    onClick = { creatingItem = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Add Item")
                }

                if (statusExpanded) {
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)) {
                        Text(
                            text = "ALL",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.filterByStatus(null)
                                    statusExpanded = false
                                }
                                .padding(12.dp)
                        )
                        ExpiryStatus.entries.forEach { status ->
                            Text(
                                text = status.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.filterByStatus(status)
                                        statusExpanded = false
                                    }
                                    .padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        editingItem?.let { item ->
            InventoryEditDialog(
                item = item,
                onDismiss = { editingItem = null },
                onSave = { quantity, storageLocation, nameOverride, almostFinished ->
                    viewModel.saveInventoryItem(
                        item = item,
                        quantity = quantity,
                        storageLocation = storageLocation,
                        nameOverride = nameOverride,
                        almostFinished = almostFinished
                    )
                    editingItem = null
                }
            )
        }

        if (creatingItem) {
            ManualInventoryEntryDialog(
                products = availableProducts,
                onDismiss = { creatingItem = false },
                onSave = { barcode, quantity, unit, expiryDate, dateBought, storageLocation, nameOverride ->
                    viewModel.createInventoryItem(
                        barcode = barcode,
                        quantity = quantity,
                        unit = unit,
                        expiryDate = expiryDate,
                        dateBought = dateBought,
                        storageLocation = storageLocation,
                        nameOverride = nameOverride,
                    )
                    creatingItem = false
                }
            )
        }

        when {
            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error: $errorMessage", color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.reloadInventory() }) {
                            Text("Retry")
                        }
                    }
                }
            }

            filteredItems.isEmpty() && isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            filteredItems.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No inventory items yet")
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    items(filteredItems) { item ->
                        InventoryListItem(
                            item = item,
                            onDelete = { viewModel.deleteItem(it.id) },
                            onClick = { editingItem = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualInventoryEntryDialog(
    products: List<Product>,
    onDismiss: () -> Unit,
    onSave: (String, Float, String, java.time.LocalDateTime?, java.time.LocalDateTime?, String?, String?) -> Unit,
) {
    var productSearch by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var quantityText by remember { mutableStateOf("1") }
    var unitText by remember { mutableStateOf("unit") }
    var expiryDateText by remember { mutableStateOf("") }
    var dateBoughtText by remember { mutableStateOf("") }
    var storageLocation by remember { mutableStateOf("") }
    var nameOverride by remember { mutableStateOf("") }
    var showValidation by remember { mutableStateOf(false) }

    val productError = if (showValidation && selectedProduct == null) "Choose a product" else null
    val quantityError = if (showValidation && quantityText.toPositiveFloatOrNull() == null) "Enter a quantity greater than 0" else null
    val unitError = if (showValidation && unitText.isBlank()) "Unit is required" else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Inventory Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (products.isEmpty()) {
                    Text("No products exist yet. Add a product from the Barcode screen first.")
                } else {
                    Text(
                        text = "Product",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SearchableProductSelector(
                        products = products,
                        searchQuery = productSearch,
                        onSearchQueryChange = { productSearch = it },
                        selectedProduct = selectedProduct,
                        onProductSelected = { selectedProduct = it },
                        label = "Choose Product"
                    )
                    productError?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Text(
                        text = "Inventory Details",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Quantity") },
                        isError = quantityError != null,
                        supportingText = quantityError?.let { message -> { Text(message) } },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = unitText,
                        onValueChange = { unitText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Unit") },
                        isError = unitError != null,
                        supportingText = unitError?.let { message -> { Text(message) } },
                        singleLine = true
                    )
                    DatePickerField(
                        label = "Expiry Date",
                        value = expiryDateText,
                        onValueChange = { expiryDateText = it }
                    )
                    DatePickerField(
                        label = "Date Bought",
                        value = dateBoughtText,
                        onValueChange = { dateBoughtText = it }
                    )
                    OutlinedTextField(
                        value = storageLocation,
                        onValueChange = { storageLocation = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Storage Location") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = nameOverride,
                        onValueChange = { nameOverride = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Display Name Override") },
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    showValidation = true
                    val product = selectedProduct ?: return@TextButton
                    val quantity = quantityText.toPositiveFloatOrNull() ?: return@TextButton
                    val unit = unitText.trim().takeIf { it.isNotEmpty() } ?: return@TextButton
                    onSave(
                        product.barcode,
                        quantity,
                        unit,
                        parseOptionalDate(expiryDateText),
                        parseOptionalDate(dateBoughtText),
                        storageLocation.trim().ifEmpty { null },
                        nameOverride.trim().ifEmpty { null },
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun parseOptionalDate(value: String): java.time.LocalDateTime? {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) {
        return null
    }

    return runCatching {
        LocalDate.parse(trimmed).atStartOfDay()
    }.getOrNull()
}

@Composable
private fun InventoryEditDialog(
    item: InventoryItem,
    onDismiss: () -> Unit,
    onSave: (Float, String?, String?, Boolean) -> Unit
) {
    var quantityText by remember(item) { mutableStateOf(item.quantity.toString()) }
    var storageLocation by remember(item) { mutableStateOf(item.storageLocation.orEmpty()) }
    var nameOverride by remember(item) { mutableStateOf(item.nameOverride.orEmpty()) }
    var almostFinished by remember(item) { mutableStateOf(item.almostFinished) }
    var showValidation by remember(item) { mutableStateOf(false) }

    val quantityError = if (showValidation && quantityText.toPositiveFloatOrNull() == null) "Enter a quantity greater than 0" else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Inventory Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Item Details",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(item.displayName(), style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("Quantity") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = quantityError != null,
                    supportingText = quantityError?.let { message -> { Text(message) } },
                    singleLine = true
                )
                OutlinedTextField(
                    value = storageLocation,
                    onValueChange = { storageLocation = it },
                    label = { Text("Storage Location") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = nameOverride,
                    onValueChange = { nameOverride = it },
                    label = { Text("Custom Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = almostFinished,
                        onCheckedChange = { almostFinished = it }
                    )
                    Text("Mark as almost finished")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    showValidation = true
                    val quantity = quantityText.toPositiveFloatOrNull() ?: return@TextButton
                    onSave(
                        quantity,
                        storageLocation.trim().ifEmpty { null },
                        nameOverride.trim().ifEmpty { null },
                        almostFinished
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun String.toPositiveFloatOrNull(): Float? = toFloatOrNull()?.takeIf { it > 0f }
