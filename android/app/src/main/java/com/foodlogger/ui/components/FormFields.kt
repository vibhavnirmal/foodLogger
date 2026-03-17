package com.foodlogger.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.foodlogger.domain.model.Product
import java.time.LocalDate

@Composable
fun DatePickerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val initialDate = runCatching { LocalDate.parse(value) }.getOrNull() ?: LocalDate.now()

    OutlinedTextField(
        value = value,
        onValueChange = {},
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        onValueChange(LocalDate.of(year, month + 1, dayOfMonth).toString())
                    },
                    initialDate.year,
                    initialDate.monthValue - 1,
                    initialDate.dayOfMonth
                ).show()
            },
        label = { Text(label) },
        placeholder = { Text("Select date") },
        readOnly = true,
        trailingIcon = {
            if (value.isNotBlank()) {
                TextButton(onClick = { onValueChange("") }) {
                    Text("Clear")
                }
            }
        }
    )
}

@Composable
fun SearchableProductSelector(
    products: List<Product>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedProduct: Product?,
    onProductSelected: (Product) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Find Product",
    excludeBarcodes: Set<String> = emptySet(),
) {
    val filteredProducts = products.filter { product ->
        product.barcode !in excludeBarcodes && (
            searchQuery.isBlank() ||
                product.name.contains(searchQuery, ignoreCase = true) ||
                (product.brand?.contains(searchQuery, ignoreCase = true) == true) ||
                product.barcode.contains(searchQuery)
        )
    }.take(8)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            placeholder = { Text("Search by name, brand, or barcode") },
            singleLine = true
        )

        if (selectedProduct != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Selected: ${selectedProduct.name}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (!selectedProduct.brand.isNullOrBlank()) {
                        Text(
                            text = selectedProduct.brand,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = selectedProduct.barcode,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        if (filteredProducts.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                ) {
                    items(filteredProducts) { product ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onProductSelected(product) }
                                .padding(12.dp)
                        ) {
                            Text(product.name, style = MaterialTheme.typography.titleSmall)
                            if (!product.brand.isNullOrBlank()) {
                                Text(
                                    text = product.brand,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = product.barcode,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}