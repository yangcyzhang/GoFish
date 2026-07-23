package com.yangcy.gofish.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yangcy.gofish.data.model.FishingSpot
import com.yangcy.gofish.ui.viewmodel.FishingLocation

@Composable
expect fun MapComponent(
    modifier: Modifier,
    spots: List<FishingSpot>,
    selectedLocation: FishingLocation,
    zoomLevel: Float,
    isSatellite: Boolean,
    pendingLocation: Pair<Double, Double>? = null, // 长按等待确认的临时点
    onMapClick: () -> Unit,
    onMapLongClick: (lat: Double, lon: Double) -> Unit,
    onMarkerClick: (FishingSpot) -> Unit
)
