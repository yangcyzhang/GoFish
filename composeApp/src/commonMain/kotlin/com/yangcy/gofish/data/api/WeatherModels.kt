package com.yangcy.gofish.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherResponse(
    @SerialName("latitude") val latitude: Double,
    @SerialName("longitude") val longitude: Double,
    @SerialName("current_weather") val currentWeather: CurrentWeather? = null
)

@Serializable
data class CurrentWeather(
    @SerialName("temperature") val temperature: Double,
    @SerialName("windspeed") val windSpeed: Double,
    @SerialName("winddirection") val windDirection: Double,
    @SerialName("weathercode") val weatherCode: Int,
    @SerialName("time") val time: String
)
