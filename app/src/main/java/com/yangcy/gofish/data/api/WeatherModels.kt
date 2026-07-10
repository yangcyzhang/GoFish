package com.yangcy.gofish.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WeatherResponse(
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "current_weather") val currentWeather: CurrentWeather?
)

@JsonClass(generateAdapter = true)
data class CurrentWeather(
    @Json(name = "temperature") val temperature: Double,
    @Json(name = "windspeed") val windSpeed: Double,
    @Json(name = "winddirection") val windDirection: Double,
    @Json(name = "weathercode") val weatherCode: Int,
    @Json(name = "time") val time: String
)
