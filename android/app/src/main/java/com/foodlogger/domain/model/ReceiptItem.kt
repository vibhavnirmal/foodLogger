package com.foodlogger.domain.model

data class ReceiptItem(
    val id: String,
    val name: String,
    val price: Float? = null,
    val quantity: Int = 1,
    val isSelected: Boolean = false,
    val productExists: Boolean = false,
    val isManual: Boolean = false
)
