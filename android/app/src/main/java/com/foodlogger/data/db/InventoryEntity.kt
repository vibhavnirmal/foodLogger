package com.foodlogger.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "inventory",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["barcode"],
            childColumns = ["barcode"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("barcode"),
        Index("expiryDate"),
        Index("boughtFromStoreId")
    ]
)
data class InventoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val barcode: String,
    val quantity: Float = 1.0f,
    val unit: String = "unit",
    val dateBought: LocalDateTime?,
    val expiryDate: LocalDateTime?,
    val storageLocation: String?,
    val boughtFromStoreId: Int?,
    val nameOverride: String?,
    val almostFinished: Boolean = false,
    val dateCreated: LocalDateTime = LocalDateTime.now(),
)
