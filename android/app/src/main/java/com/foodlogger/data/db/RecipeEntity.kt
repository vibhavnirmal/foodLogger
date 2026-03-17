package com.foodlogger.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

enum class TimeType {
    VERY_FAST,
    MODERATE,
    SLOW
}

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val timeType: TimeType = TimeType.MODERATE,
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
