package com.foodlogger.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "storage_locations",
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class StorageLocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
)
