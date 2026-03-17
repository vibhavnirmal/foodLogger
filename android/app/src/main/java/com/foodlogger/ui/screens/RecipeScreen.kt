package com.foodlogger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.foodlogger.domain.model.Product
import com.foodlogger.domain.model.Recipe
import com.foodlogger.domain.model.RecipeIngredientDraft
import com.foodlogger.domain.model.TimeType
import com.foodlogger.ui.components.RecipeListItem
import com.foodlogger.ui.components.SearchableProductSelector
import com.foodlogger.ui.viewmodel.RecipeViewModel

@Composable
fun RecipeScreen(viewModel: RecipeViewModel = hiltViewModel()) {
    val recipes by viewModel.recipes.collectAsState()
    val availableProducts by viewModel.availableProducts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val timeTypeFilter by viewModel.timeTypeFilter.collectAsState()

    val filteredRecipes = if (timeTypeFilter != null) {
        recipes.filter { it.timeType == timeTypeFilter }
    } else {
        recipes
    }

    var creatingRecipe by remember { mutableStateOf(false) }
    var selectedRecipe by remember { mutableStateOf<Recipe?>(null) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Recipes",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Text(
                    text = "Cook Time",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                RecipeTimeTypeSelector(
                    selected = timeTypeFilter,
                    onSelected = { viewModel.setTimeTypeFilter(it) }
                )

                Button(
                    onClick = { creatingRecipe = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Add Recipe")
                }
            }
        }
    ) { paddingValues ->
        if (creatingRecipe) {
            RecipeCreateDialog(
                products = availableProducts,
                onDismiss = { creatingRecipe = false },
                onSave = { name, timeType, ingredients ->
                    viewModel.createRecipe(name, timeType, ingredients)
                    creatingRecipe = false
                }
            )
        }

        selectedRecipe?.let { recipe ->
            RecipeDetailsDialog(
                recipe = recipe,
                onDismiss = { selectedRecipe = null },
                onDelete = {
                    viewModel.deleteRecipeItem(recipe.id)
                    selectedRecipe = null
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
                        Button(onClick = { viewModel.reloadRecipes() }) {
                            Text("Retry")
                        }
                    }
                }
            }

            filteredRecipes.isEmpty() && isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            filteredRecipes.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No recipes found")
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    items(filteredRecipes) { recipe ->
                        RecipeListItem(
                            recipe = recipe,
                            onClick = { selectedRecipe = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeCreateDialog(
    products: List<Product>,
    onDismiss: () -> Unit,
    onSave: (String, TimeType, List<RecipeIngredientDraft>) -> Unit,
) {
    var recipeName by remember { mutableStateOf("") }
    var timeType by remember { mutableStateOf(TimeType.MODERATE) }
    var productSearch by remember { mutableStateOf("") }
    var selectedIngredients by remember { mutableStateOf(listOf<EditableRecipeIngredient>()) }
    var showValidation by remember { mutableStateOf(false) }

    val recipeNameError = if (showValidation && recipeName.isBlank()) "Recipe name is required" else null
    val ingredientsError = if (showValidation && selectedIngredients.isEmpty()) "Add at least one ingredient" else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Recipe") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FormSectionTitle("Recipe Basics")
                OutlinedTextField(
                    value = recipeName,
                    onValueChange = { recipeName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Recipe Name") },
                    isError = recipeNameError != null,
                    supportingText = recipeNameError?.let { message -> { Text(message) } },
                    singleLine = true
                )

                RecipeTimeTypeSelector(
                    selected = timeType,
                    onSelected = { selected -> timeType = selected ?: TimeType.MODERATE },
                    includeAll = false
                )

                FormSectionTitle("Ingredients")
                SearchableProductSelector(
                    products = products,
                    searchQuery = productSearch,
                    onSearchQueryChange = { productSearch = it },
                    selectedProduct = null,
                    onProductSelected = { product ->
                        selectedIngredients = selectedIngredients + EditableRecipeIngredient(
                            barcode = product.barcode,
                            quantityText = "1",
                            unit = "unit"
                        )
                        productSearch = ""
                    },
                    label = "Add Ingredient",
                    excludeBarcodes = selectedIngredients.map { it.barcode }.toSet()
                )
                ingredientsError?.let { message ->
                    ValidationText(message)
                }

                selectedIngredients.forEach { ingredient ->
                    val productName = products.firstOrNull { it.barcode == ingredient.barcode }?.name ?: ingredient.barcode
                    val quantityError = ingredient.quantityText.toPositiveFloatOrNull()?.let { null }
                        ?: "Enter a quantity greater than 0"
                    val unitError = if (ingredient.unit.isBlank()) "Unit is required" else null
                    IngredientDraftCard(
                        productName = productName,
                        ingredient = ingredient,
                        quantityError = if (showValidation) quantityError else null,
                        unitError = if (showValidation) unitError else null,
                        onDecrease = {
                            selectedIngredients = selectedIngredients.map {
                                if (it.barcode == ingredient.barcode) {
                                    val currentQuantity = it.quantityText.toPositiveFloatOrNull() ?: 1f
                                    it.copy(quantityText = (currentQuantity - 1f).coerceAtLeast(0.5f).formatQuantity())
                                } else {
                                    it
                                }
                            }
                        },
                        onIncrease = {
                            selectedIngredients = selectedIngredients.map {
                                if (it.barcode == ingredient.barcode) {
                                    val currentQuantity = it.quantityText.toPositiveFloatOrNull() ?: 0f
                                    it.copy(quantityText = (currentQuantity + 1f).formatQuantity())
                                } else {
                                    it
                                }
                            }
                        },
                        onQuantityChange = { value ->
                            selectedIngredients = selectedIngredients.map {
                                if (it.barcode == ingredient.barcode) {
                                    it.copy(quantityText = value)
                                } else {
                                    it
                                }
                            }
                        },
                        onUnitChange = { value ->
                            selectedIngredients = selectedIngredients.map {
                                if (it.barcode == ingredient.barcode) {
                                    it.copy(unit = value)
                                } else {
                                    it
                                }
                            }
                        },
                        onRemove = {
                            selectedIngredients = selectedIngredients.filterNot { it.barcode == ingredient.barcode }
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    showValidation = true
                    val hasValidIngredients = selectedIngredients.all {
                        it.quantityText.toPositiveFloatOrNull() != null && it.unit.isNotBlank()
                    }
                    if (recipeName.isBlank() || selectedIngredients.isEmpty() || !hasValidIngredients) {
                        return@TextButton
                    }

                    onSave(
                        recipeName.trim(),
                        timeType,
                        selectedIngredients.map {
                            RecipeIngredientDraft(
                                barcode = it.barcode,
                                quantity = it.quantityText.toPositiveFloatOrNull()!!,
                                unit = it.unit.trim()
                            )
                        }
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

@Composable
private fun IngredientDraftCard(
    productName: String,
    ingredient: EditableRecipeIngredient,
    quantityError: String?,
    unitError: String?,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onQuantityChange: (String) -> Unit,
    onUnitChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(productName, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = ingredient.barcode,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove ingredient")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDecrease) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity")
                }
                OutlinedTextField(
                    value = ingredient.quantityText,
                    onValueChange = onQuantityChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("Qty") },
                    isError = quantityError != null,
                    supportingText = quantityError?.let { message -> { Text(message) } },
                    singleLine = true
                )
                IconButton(onClick = onIncrease) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase quantity")
                }
            }

            OutlinedTextField(
                value = ingredient.unit,
                onValueChange = onUnitChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Unit") },
                isError = unitError != null,
                supportingText = unitError?.let { message -> { Text(message) } },
                singleLine = true
            )
        }
    }
}

@Composable
private fun RecipeTimeTypeSelector(
    selected: TimeType?,
    onSelected: (TimeType?) -> Unit,
    includeAll: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (includeAll) {
            SegmentedChoice(
                label = "All",
                selected = selected == null,
                modifier = Modifier.weight(1f),
                onClick = { onSelected(null) }
            )
        }
        TimeType.entries.forEach { option ->
            SegmentedChoice(
                label = option.name.lowercase().replace('_', ' '),
                selected = selected == option,
                modifier = Modifier.weight(1f),
                onClick = { onSelected(option) }
            )
        }
    }
}

@Composable
private fun SegmentedChoice(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        modifier = modifier
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        style = MaterialTheme.typography.labelLarge
    )
}

@Composable
private fun FormSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun RecipeDetailsDialog(
    recipe: Recipe,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(recipe.name, style = MaterialTheme.typography.headlineSmall)
                            Text(
                                text = "Cook Time: ${recipe.timeType.name.lowercase().replace('_', ' ')}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close recipe details")
                        }
                    }
                },
                bottomBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = onDelete,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null)
                            Text("Delete")
                        }
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Close")
                        }
                    }
                }
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Overview", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    text = "${recipe.ingredients.size} ingredient${if (recipe.ingredients.size == 1) "" else "s"}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                    item {
                        Text(
                            text = "Ingredients",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    items(recipe.ingredients) { ingredient ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ingredient.productName,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = ingredient.barcode,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${ingredient.quantity.formatQuantity()} ${ingredient.unit}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

private data class EditableRecipeIngredient(
    val barcode: String,
    val quantityText: String,
    val unit: String,
)

@Composable
private fun ValidationText(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error
    )
}

private fun String.toPositiveFloatOrNull(): Float? = toFloatOrNull()?.takeIf { it > 0f }

private fun Float.formatQuantity(): String {
    return if (this % 1f == 0f) {
        toInt().toString()
    } else {
        toString()
    }
}
