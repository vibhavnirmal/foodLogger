package com.foodlogger.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey
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
