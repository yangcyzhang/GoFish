package com.yangcy.gofish.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yangcy.gofish.data.api.ApiClient
import com.yangcy.gofish.data.model.CatchLog
import com.yangcy.gofish.data.model.Fish
import com.yangcy.gofish.data.model.FishData
import com.yangcy.gofish.data.model.FishingSpot
import com.yangcy.gofish.data.repository.CatchRepository
import com.yangcy.gofish.data.repository.FishingSpotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FishingLocation(
    val name: String,
    val province: String,
    val lat: Double,
    val lon: Double,
    val prominentSpecies: List<String>,
    val description: String
)

class FishViewModel(
    private val catchRepository: CatchRepository,
    private val fishingSpotRepository: FishingSpotRepository,
    private val locationProvider: LocationProvider? = null
) : ViewModel() {

    val catchLogs: StateFlow<List<CatchLog>> = catchRepository.allCatches
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val fishingSpots: StateFlow<List<FishingSpot>> = fishingSpotRepository.allFishingSpots
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val defaultLocation = FishingLocation(
        name = "南山区西丽水域",
        province = "广东",
        lat = 22.5976,
        lon = 113.9576,
        prominentSpecies = listOf("crucian_carp", "culter", "largemouth_bass"),
        description = "默认高精度定位。附近最适合垂钓的水域为南山区西丽水库，此处环境优美，是深圳资深钓友的常去之地。"
    )

    private val _isPositioning = MutableStateFlow(false)
    val isPositioning = _isPositioning.asStateFlow()
    
    private var lastLocationRequestTime = 0L
    private val LOCATION_COOLDOWN_MS = 5000L

    private val _selectedLocation = MutableStateFlow(defaultLocation)
    val selectedLocation = _selectedLocation.asStateFlow()

    fun triggerPreciseLocation(onFrequentRequest: () -> Unit = {}, onResult: (String) -> Unit = {}) {
        if (_isPositioning.value) return
        
        val currentTime = getTimeMillis()
        if (currentTime - lastLocationRequestTime < LOCATION_COOLDOWN_MS) {
            onFrequentRequest()
            return
        }
        
        _isPositioning.value = true
        lastLocationRequestTime = currentTime
        
        viewModelScope.launch {
            try {
                val location = locationProvider?.getCurrentLocation()
                val lat = location?.latitude ?: 22.5431
                val lon = location?.longitude ?: 114.0579
                
                val locationName = locationProvider?.getAddressFromLocation(lat, lon) ?: "当前位置"

                val newLoc = FishingLocation(
                    name = locationName,
                    province = "本地水域",
                    lat = lat,
                    lon = lon,
                    prominentSpecies = listOf("crucian_carp", "culter", "largemouth_bass", "common_carp", "grass_carp", "snakehead").shuffled().take(3),
                    description = "已为您高精度定位至：附近最适合垂钓的水域为 $locationName，此处环境优美，鱼情活跃。"
                )
                
                _selectedLocation.value = newLoc
                fetchWeatherForLocation(newLoc)
                onResult(locationName)
            } catch (e: Exception) {
                // Log error
            } finally {
                _isPositioning.value = false
            }
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow("全部")
    val selectedFilter = _selectedFilter.asStateFlow()

    val filteredFishList: StateFlow<List<Fish>> = combine(
        _searchQuery,
        _selectedFilter
    ) { query, filter ->
        var list = FishData.fishList
        list = when (filter) {
            "路亚鱼种" -> list.filter { it.category == "路亚" }
            "手竿鱼种" -> list.filter { it.category == "手竿" }
            "海竿鱼种" -> list.filter { it.category == "海竿" }
            else -> list
        }
        if (query.isNotBlank()) {
            list = list.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.scientificName.contains(query, ignoreCase = true) ||
                        it.description.contains(query, ignoreCase = true) ||
                        it.habitat.contains(query, ignoreCase = true)
            }
        }
        list
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FishData.fishList
    )

    private val _weatherLoading = MutableStateFlow(false)
    val weatherLoading = _weatherLoading.asStateFlow()

    private val _weatherTemp = MutableStateFlow("24.0°C")
    val weatherTemp = _weatherTemp.asStateFlow()

    private val _weatherCondition = MutableStateFlow("多云")
    val weatherCondition = _weatherCondition.asStateFlow()

    private val _weatherWind = MutableStateFlow("3.5 m/s")
    val weatherWind = _weatherWind.asStateFlow()

    private val _weatherError = MutableStateFlow<String?>(null)
    val weatherError = _weatherError.asStateFlow()

    init {
        fetchWeatherForLocation(_selectedLocation.value)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun selectLocation(location: FishingLocation) {
        _selectedLocation.value = location
        fetchWeatherForLocation(location)
    }

    fun fetchWeatherForLocation(location: FishingLocation) {
        viewModelScope.launch {
            _weatherLoading.value = true
            _weatherError.value = null
            try {
                val response = ApiClient.getWeather(location.lat, location.lon)
                response.currentWeather?.let {
                    _weatherTemp.value = "${it.temperature}°C"
                    _weatherCondition.value = translateWeatherCode(it.weatherCode)
                    _weatherWind.value = "${it.windSpeed} m/s"
                } ?: run {
                    _weatherError.value = "无法获取具体钓况"
                }
            } catch (e: Exception) {
                _weatherError.value = "网络异常，已启用本地推荐"
                _weatherTemp.value = "26.5°C"
                _weatherCondition.value = "多云微风"
                _weatherWind.value = "2.8 m/s"
            } finally {
                _weatherLoading.value = false
            }
        }
    }

    private fun translateWeatherCode(code: Int): String {
        return when (code) {
            0 -> "晴朗"
            1, 2, 3 -> "多云"
            45, 48 -> "大雾"
            51, 53, 55 -> "细雨"
            61, 63, 65 -> "中雨"
            71, 73, 75 -> "小雪"
            80, 81, 82 -> "雷阵雨"
            else -> "多云微风"
        }
    }

    fun getDynamicFishingTip(fish: Fish): String {
        val tempVal = _weatherTemp.value.replace("°C", "").toDoubleOrNull() ?: 24.0
        val cond = _weatherCondition.value

        return when {
            tempVal < 15.0 -> "【低温钓况分析】当前温度低（${_weatherTemp.value}），鱼口偏轻活性弱。建议减细线组，使用极高蛋白质饵料，并死守深水底层。"
            tempVal > 30.0 -> "【高温钓况分析】水温极高（${_weatherTemp.value}），建议选择早晚清晨或夜钓，钓竿采用长竿钓深水，饵料建议偏清淡。"
            cond.contains("雨") -> "【雨天钓况分析】当前伴随“${cond}”，中上层鱼类异常活跃，是路亚和飞铅钓浮的黄金时机！"
            else -> "【推荐黄金钓况】当前天气“${cond}”且温度适宜（${_weatherTemp.value}），风力为${_weatherWind.value}。建议使用${fish.rodRecommendation}，使用${fish.baitRecommendation}。"
        }
    }

    fun addCatchLog(fishName: String, location: String, weight: Double, length: Double, bait: String, notes: String) {
        viewModelScope.launch {
            val newLog = CatchLog(
                fishName = fishName,
                location = location,
                weightKg = weight,
                lengthCm = length,
                baitUsed = bait,
                notes = notes,
                weatherCondition = _weatherCondition.value,
                temperature = _weatherTemp.value,
                timestamp = getTimeMillis()
            )
            catchRepository.insert(newLog)
        }
    }

    fun deleteCatchLog(log: CatchLog) {
        viewModelScope.launch { catchRepository.delete(log) }
    }

    fun addFishingSpot(spot: FishingSpot) {
        viewModelScope.launch { fishingSpotRepository.insert(spot) }
    }

    fun deleteFishingSpot(spot: FishingSpot) {
        viewModelScope.launch { fishingSpotRepository.delete(spot) }
    }
}

// Expect fun for getting time
expect fun getTimeMillis(): Long
