package com.foodlogger.domain.model

import java.time.LocalDateTime

enum class ExpiryStatus {
    EXPIRED,
    EXPIRING_SOON,
    GOOD
}

data class InventoryItem(
    val id: Int = 0,
    val barcode: String,
    val productName: String,
    val quantity: Float = 1.0f,
    val unit: String = "unit",
    val dateBought: LocalDateTime?,
    val expiryDate: LocalDateTime?,
    val storageLocation: String?,
    val nameOverride: String?,
    val almostFinished: Boolean = false,
    val dateCreated: LocalDateTime = LocalDateTime.now(),
    val expiryStatus: ExpiryStatus = ExpiryStatus.GOOD,
) {
    fun displayName(): String = nameOverride ?: productName
}
