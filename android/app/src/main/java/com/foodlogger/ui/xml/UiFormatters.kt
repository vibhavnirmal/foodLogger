package com.foodlogger.ui.xml

import android.app.DatePickerDialog
import android.content.Context
import com.foodlogger.domain.model.ExpiryStatus
import com.foodlogger.domain.model.TimeType
import com.google.android.material.textfield.TextInputEditText
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val shortDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

fun LocalDateTime?.displayDate(): String? = this?.format(shortDateFormatter)

fun String.parseOptionalDateTime(): LocalDateTime? {
    val trimmed = trim()
    if (trimmed.isEmpty()) return null
    return runCatching { LocalDate.parse(trimmed).atStartOfDay() }.getOrNull()
}

fun String.isFutureDateInput(): Boolean {
    val trimmed = trim()
    if (trimmed.isEmpty()) return false
    val parsedDate = runCatching { LocalDate.parse(trimmed) }.getOrNull() ?: return false
    return parsedDate.isAfter(LocalDate.now())
}

fun Float.formatQuantity(): String {
    return if (this % 1f == 0f) toInt().toString() else toString()
}

fun String.toPositiveFloatOrNull(): Float? = toFloatOrNull()?.takeIf { it > 0f }

fun TimeType.displayLabel(): String = name.lowercase().replace('_', ' ')

fun ExpiryStatus.displayLabel(): String = name.replace('_', ' ')

fun Context.attachDatePicker(editText: TextInputEditText) {
    editText.setOnClickListener {
        val current = runCatching { LocalDate.parse(editText.text?.toString().orEmpty()) }.getOrNull() ?: LocalDate.now()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                editText.setText(LocalDate.of(year, month + 1, dayOfMonth).toString())
            },
            current.year,
            current.monthValue - 1,
            current.dayOfMonth
        ).show()
    }
    editText.setOnFocusChangeListener { _, hasFocus ->
        if (hasFocus) {
            editText.performClick()
        }
    }
}