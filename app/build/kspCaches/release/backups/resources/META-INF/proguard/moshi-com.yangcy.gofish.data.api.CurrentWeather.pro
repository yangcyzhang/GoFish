-keepnames class com.yangcy.gofish.data.api.CurrentWeather
-if class com.yangcy.gofish.data.api.CurrentWeather
-keep class com.yangcy.gofish.data.api.CurrentWeatherJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
