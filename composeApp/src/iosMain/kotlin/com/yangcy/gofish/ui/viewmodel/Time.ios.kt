package com.yangcy.gofish.ui.viewmodel

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.timeIntervalSince1970

actual fun getTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

actual fun formatTimestamp(timestamp: Long): String {
    val date = NSDate(timestamp.toDouble() / 1000.0)
    val formatter = NSDateFormatter()
    formatter.dateFormat = "yyyy-MM-dd HH:mm"
    return formatter.stringFromDate(date)
}

actual fun formatDecimal(value: Double): String {
    val formatter = NSNumberFormatter()
    formatter.minimumFractionDigits = 1u
    formatter.maximumFractionDigits = 1u
    return formatter.stringFromNumber(NSNumber(value)) ?: value.toString()
}
