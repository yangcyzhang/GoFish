package com.yangcy.gofish.ui.viewmodel

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.CLGeocoder
import platform.CoreLocation.CLPlacemark
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSError
import platform.darwin.NSObject
import platform.UIKit.UIApplication
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
class IosLocationProvider : LocationProvider {
    // 关键修复：延迟初始化，确保在主线程使用时才创建
    private val locationManager by lazy { CLLocationManager() }
    private val geocoder = CLGeocoder()
    private val delegate = LocationDelegate()

    override suspend fun getCurrentLocation(): PlatformLocation? = suspendCancellableCoroutine { continuation ->
        delegate.onLocationUpdate = { location ->
            continuation.resume(location)
        }
        locationManager.delegate = delegate
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.requestWhenInUseAuthorization()
        locationManager.startUpdatingLocation()
        
        continuation.invokeOnCancellation {
            locationManager.stopUpdatingLocation()
            delegate.onLocationUpdate = null
        }
    }

    override suspend fun getAddressFromLocation(lat: Double, lon: Double): String? = suspendCancellableCoroutine { continuation ->
        val location = CLLocation(latitude = lat, longitude = lon)
        geocoder.reverseGeocodeLocation(location) { placemarks, error ->
            if (error != null || placemarks.isNullOrEmpty()) {
                continuation.resume(null)
            } else {
                val placemark = placemarks.first() as CLPlacemark
                val address = "${placemark.locality ?: ""}${placemark.subLocality ?: placemark.name ?: ""}"
                continuation.resume(address.ifEmpty { "未知地点" })
            }
        }
    }

    private class LocationDelegate : NSObject(), CLLocationManagerDelegateProtocol {
        var onLocationUpdate: ((PlatformLocation?) -> Unit)? = null

        override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
            val location = didUpdateLocations.lastOrNull() as? CLLocation
            if (location != null) {
                val coord = location.coordinate.useContents {
                    PlatformLocation(latitude, longitude)
                }
                onLocationUpdate?.invoke(coord)
                onLocationUpdate = null
                manager.stopUpdatingLocation()
            }
        }

        override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
            onLocationUpdate?.invoke(null)
            onLocationUpdate = null
            manager.stopUpdatingLocation()
        }
    }
}
