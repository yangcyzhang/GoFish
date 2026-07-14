package com.yangcy.gofish.util

expect fun showToast(message: String)

interface AnalyticsProvider {
    fun trackEvent(eventId: String, properties: Map<String, Any>? = null)
    fun onPageStart(pageName: String)
    fun onPageEnd(pageName: String)
}

expect val analytics: AnalyticsProvider
