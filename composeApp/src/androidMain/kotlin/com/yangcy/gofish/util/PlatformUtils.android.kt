package com.yangcy.gofish.util

import android.widget.Toast
import com.yangcy.gofish.data.database.appContext

actual fun showToast(message: String) {
    Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
}

actual val analytics: AnalyticsProvider = object : AnalyticsProvider {
    override fun trackEvent(eventId: String, properties: Map<String, Any>?) {
        AnalyticsManager.trackEvent(appContext, eventId, properties)
    }

    override fun onPageStart(pageName: String) {
        AnalyticsManager.onPageStart(pageName)
    }

    override fun onPageEnd(pageName: String) {
        AnalyticsManager.onPageEnd(pageName)
    }
}
