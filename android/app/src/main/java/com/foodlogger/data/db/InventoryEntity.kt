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
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ReceiptEntity::class,
            parentColumns = ["id"],
            childColumns = ["receiptId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("productId"),
        Index("expiryDate"),
        Index("boughtFromStoreId"),
        Index("receiptId")
    ]
)
data class InventoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val productId: Int,
    val quantity: Float = 1.0f,
    val unit: String = "unit",
    val dateBought: LocalDateTime?,
    val expiryDate: LocalDateTime?,
    val storageLocation: String?,
    val boughtFromStoreId: Int?,
    val nameOverride: String?,
    val almostFinished: Boolean = false,
    val imageUri: String? = null,
    val dateCreated: LocalDateTime = LocalDateTime.now(),
    val receiptId: Int? = null
)
