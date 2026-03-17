package com.foodlogger.domain.model

import java.time.LocalDateTime

data class Product(
    val barcode: String,
    val name: String,
    val brand: String?,
    val category: String?,
    val servingSize: String?,
    val kcal: Float?,
    val protein: Float?,
    val carbs: Float?,
    val fat: Float?,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val lastUpdated: LocalDateTime = LocalDateTime.now(),
)
