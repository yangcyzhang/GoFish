package com.yangcy.gofish.ui.viewmodel

import android.content.Context
import android.location.Geocoder
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

class AndroidLocationProvider(private val context: Context) : LocationProvider {
    private var locationClient: AMapLocationClient? = null
    
    private val locationOption = AMapLocationClientOption().apply {
        locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
        isOnceLocation = true
        isOnceLocationLatest = true
    }

    override suspend fun getCurrentLocation(): PlatformLocation? = suspendCancellableCoroutine { continuation ->
        try {
            if (locationClient == null) {
                locationClient = AMapLocationClient(context)
            }
            val client = locationClient!!
            client.setLocationOption(locationOption)
            client.setLocationListener { amapLocation ->
                if (continuation.isActive) {
                    client.stopLocation()
                    if (amapLocation != null && amapLocation.errorCode == 0) {
                        continuation.resume(PlatformLocation(amapLocation.latitude, amapLocation.longitude))
                    } else {
                        continuation.resume(null)
                    }
                }
            }
            client.startLocation()
            continuation.invokeOnCancellation { client.stopLocation() }
        } catch (e: Exception) {
            if (continuation.isActive) {
                continuation.resume(null)
            }
        }
    }

    override suspend fun getAddressFromLocation(lat: Double, lon: Double): String? {
        return try {
            val geocoder = Geocoder(context, Locale.CHINA)
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                addr.featureName ?: addr.thoroughfare ?: addr.subLocality ?: addr.locality
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
