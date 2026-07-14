-keepnames class com.yangcy.gofish.data.api.WeatherResponse
-if class com.yangcy.gofish.data.api.WeatherResponse
-keep class com.yangcy.gofish.data.api.WeatherResponseJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
