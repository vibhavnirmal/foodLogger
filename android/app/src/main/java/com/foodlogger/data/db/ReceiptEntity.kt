package com.foodlogger.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "receipts")
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val imagePath: String,
    val dateShopped: LocalDateTime? = null,
    val storeId: Int? = null,
    val dateScanned: LocalDateTime = LocalDateTime.now(),
    val storeName: String? = null,
    val totalAmount: Float? = null
)
