package com.foodlogger.domain.model

import java.time.LocalDateTime

enum class ExpiryStatus {
    EXPIRED,
    EXPIRING_SOON,
    GOOD
}

data class InventoryItem(
    val id: Int = 0,
    val productId: Int,
    val barcode: String?,
    val productName: String,
    val quantity: Float = 1.0f,
    val unit: String = "unit",
    val dateBought: LocalDateTime?,
    val expiryDate: LocalDateTime?,
    val storageLocation: String?,
    val boughtFromStoreId: Int? = null,
    val boughtFromStoreName: String? = null,
    val boughtFromStoreImageUri: String? = null,
    val nameOverride: String?,
    val almostFinished: Boolean = false,
    val imageUri: String? = null,
    val dateCreated: LocalDateTime = LocalDateTime.now(),
    val expiryStatus: ExpiryStatus = ExpiryStatus.GOOD,
    val receiptId: Int? = null,
) {
    fun displayName(): String = nameOverride ?: productName
    fun hasReceipt(): Boolean = receiptId != null
}
