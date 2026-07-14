package com.yangcy.gofish.ui.viewmodel

class IosLocationProvider : LocationProvider {
    override suspend fun getCurrentLocation(): PlatformLocation? {
        // Implement with CoreLocation
        return PlatformLocation(22.5431, 114.0579) // Shenzhen placeholder
    }

    override suspend fun getAddressFromLocation(lat: Double, lon: Double): String? {
        return "深圳"
    }
}
