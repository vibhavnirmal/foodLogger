package com.foodlogger.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foodlogger.data.repository.FoodLoggerRepository
import com.foodlogger.domain.model.Product
import com.foodlogger.domain.model.Recipe
import com.foodlogger.domain.model.RecipeIngredientDraft
import com.foodlogger.domain.model.TimeType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecipeViewModel @Inject constructor(
    private val repository: FoodLoggerRepository
) : ViewModel() {

    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes: StateFlow<List<Recipe>> = _recipes.asStateFlow()

    private val _availableProducts = MutableStateFlow<List<Product>>(emptyList())
    val availableProducts: StateFlow<List<Product>> = _availableProducts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _timeTypeFilter = MutableStateFlow<TimeType?>(null)
    val timeTypeFilter: StateFlow<TimeType?> = _timeTypeFilter.asStateFlow()

    init {
        loadRecipes()
        loadProducts()
    }

    private fun loadRecipes() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getAllRecipes().collect { items ->
                    _recipes.value = items.sortedBy { it.name }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Unknown error loading recipes"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadProducts() {
        viewModelScope.launch {
            try {
                repository.getAllProducts().collect { items ->
                    _availableProducts.value = items.sortedBy { it.name }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error loading products"
            }
        }
    }

    fun setTimeTypeFilter(timeType: TimeType?) {
        _timeTypeFilter.value = timeType
    }

    fun getFilteredRecipes(): List<Recipe> {
        val filter = _timeTypeFilter.value
        return if (filter != null) {
            _recipes.value.filter { it.timeType == filter }
        } else {
            _recipes.value
        }
    }

    suspend fun addRecipe(
        name: String,
        timeType: TimeType,
        ingredientBarcodes: List<RecipeIngredientDraft>
    ) {
        try {
            repository.addRecipe(
                name = name,
                timeType = timeType.name,
                ingredients = ingredientBarcodes
            )
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Error adding recipe"
        }
    }

    suspend fun deleteRecipe(id: Int) {
        try {
            repository.deleteRecipe(id)
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Error deleting recipe"
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // Public convenience methods for UI
    fun reloadRecipes() {
        loadRecipes()
    }

    fun deleteRecipeItem(id: Int) {
        viewModelScope.launch {
            deleteRecipe(id)
        }
    }

    fun createRecipe(
        name: String,
        timeType: TimeType,
        ingredients: List<RecipeIngredientDraft>
    ) {
        viewModelScope.launch {
            addRecipe(name, timeType, ingredients)
        }
    }
}
