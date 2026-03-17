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
    val barcode: String,
    val quantity: Float = 1.0f,
    val unit: String = "unit",
)

data class RecipeIngredient(
    val id: Int = 0,
    val recipeId: Int,
    val barcode: String,
    val productName: String,
    val quantity: Float = 1.0f,
    val unit: String = "unit",
)
