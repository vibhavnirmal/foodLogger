package com.foodlogger.domain.model

import java.time.LocalDateTime

enum class TimeType {
    VERY_FAST,
    MODERATE,
    SLOW
}

data class Recipe(
    val id: Int = 0,
    val name: String,
    val timeType: TimeType = TimeType.MODERATE,
    val ingredients: List<RecipeIngredient> = emptyList(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
)

data class RecipeIngredientDraft(
    val productId: Int,
)

data class RecipeIngredient(
    val id: Int = 0,
    val recipeId: Int,
    val productId: Int,
    val productName: String,
)
