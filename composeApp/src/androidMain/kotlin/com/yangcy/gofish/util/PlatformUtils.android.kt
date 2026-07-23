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

actual fun openMapNavigation(lat: Double, lon: Double, name: String) {
    try {
        val uri = android.net.Uri.parse("androidamap://navi?sourceApplication=GoFish&lat=$lat&lon=$lon&dev=0&style=2")
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri).apply {
            setPackage("com.autonavi.minimap")
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (intent.resolveActivity(appContext.packageManager) != null) {
            appContext.startActivity(intent)
        } else {
            val geoUri = android.net.Uri.parse("geo:$lat,$lon?q=$lat,$lon($name)")
            val fallbackIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, geoUri).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(fallbackIntent)
        }
    } catch (e: Exception) {
        showToast("无法调起地图导航")
    }
}
