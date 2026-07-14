package com.yangcy.gofish.ui.viewmodel

interface LocationProvider {
    suspend fun getCurrentLocation(): PlatformLocation?
    suspend fun getAddressFromLocation(lat: Double, lon: Double): String?
}

data class PlatformLocation(
    val latitude: Double,
    val longitude: Double
)
