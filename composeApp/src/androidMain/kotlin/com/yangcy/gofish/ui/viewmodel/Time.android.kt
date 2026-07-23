package com.yangcy.gofish.ui.viewmodel

import java.text.SimpleDateFormat
import java.util.*

actual fun getTimeMillis(): Long = System.currentTimeMillis()

actual fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

actual fun formatDecimal(value: Double): String {
    return String.format(Locale.getDefault(), "%.1f", value)
}
