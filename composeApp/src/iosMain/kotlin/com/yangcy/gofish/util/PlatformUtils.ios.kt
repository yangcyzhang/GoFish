package com.yangcy.gofish.util

actual fun showToast(message: String) {
    // Basic iOS Toast implementation or just print
    println("Toast: $message")
}

actual val analytics: AnalyticsProvider = object : AnalyticsProvider {
    override fun trackEvent(eventId: String, properties: Map<String, Any>?) {
        // iOS Analytics implementation
    }

    override fun onPageStart(pageName: String) {
    }

    override fun onPageEnd(pageName: String) {
    }
}
