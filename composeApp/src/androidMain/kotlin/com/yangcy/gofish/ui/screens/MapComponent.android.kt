package com.yangcy.gofish.ui.screens

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.TextureMapView
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.yangcy.gofish.data.model.FishingSpot
import com.yangcy.gofish.ui.viewmodel.FishingLocation

// --- 100% 还原微信小程序 Marker：红色水滴 Pin + 头部中心白色圆点 + 底部柔和阴影 ---
private fun createBalloonMarkerBitmap(context: android.content.Context, colorHex: String): android.graphics.Bitmap {
    val size = 120 // 宽高120px
    val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    
    // 解析十六进制颜色，若为默认颜色或无法解析，则使用小程序经典的鲜艳红 (#FF5252)
    val color = try {
        if (colorHex == "#FF00ADB5" || colorHex.isEmpty()) {
            android.graphics.Color.parseColor("#FF5252")
        } else {
            android.graphics.Color.parseColor(colorHex)
        }
    } catch (e: Exception) {
        android.graphics.Color.parseColor("#FF5252")
    }
    
    // 1. 底层扁平椭圆阴影
    paint.color = android.graphics.Color.argb(70, 0, 0, 0)
    paint.style = android.graphics.Paint.Style.FILL
    canvas.drawOval(60f - 18f, 102f - 4f, 60f + 18f, 102f + 4f, paint)
    
    // 2. 小程序经典水滴 Pin 形路径
    val path = android.graphics.Path()
    path.moveTo(60f, 104f) // 最下方的尖端
    path.cubicTo(42f, 78f, 30f, 60f, 30f, 42f)
    path.arcTo(30f, 12f, 90f, 72f, 180f, 180f, false)
    path.cubicTo(90f, 60f, 78f, 78f, 60f, 104f)
    path.close()
    
    // 3. 填充 Pin 主体颜色
    paint.style = android.graphics.Paint.Style.FILL
    paint.color = color
    canvas.drawPath(path, paint)
    
    // 4. 绘制深色精致边框
    paint.color = android.graphics.Color.rgb(
        (android.graphics.Color.red(color) * 0.8).toInt(),
        (android.graphics.Color.green(color) * 0.8).toInt(),
        (android.graphics.Color.blue(color) * 0.8).toInt()
    )
    paint.style = android.graphics.Paint.Style.STROKE
    paint.strokeWidth = 2.5f
    canvas.drawPath(path, paint)
    
    // 5. 核心特征：在头部圆心 (60, 42) 绘制小程序标志性的白色内实心圆点
    paint.color = android.graphics.Color.WHITE
    paint.style = android.graphics.Paint.Style.FILL
    canvas.drawCircle(60f, 42f, 8.5f, paint)
    
    return bitmap
}

@Composable
actual fun MapComponent(
    modifier: Modifier,
    spots: List<FishingSpot>,
    selectedLocation: FishingLocation,
    zoomLevel: Float,
    isSatellite: Boolean,
    pendingLocation: Pair<Double, Double>?,
    onMapClick: () -> Unit,
    onMapLongClick: (lat: Double, lon: Double) -> Unit,
    onMarkerClick: (FishingSpot) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val mapView = remember { TextureMapView(context) }
    var aMap by remember { mutableStateOf<AMap?>(null) }

    LaunchedEffect(selectedLocation, zoomLevel, aMap) {
        aMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(selectedLocation.lat, selectedLocation.lon), zoomLevel))
    }
    
    // 同步地图模式
    LaunchedEffect(isSatellite, aMap) {
        aMap?.mapType = if (isSatellite) AMap.MAP_TYPE_SATELLITE else AMap.MAP_TYPE_NORMAL
    }

    AndroidView(
        factory = { 
            mapView.onCreate(null)
            mapView.onResume()
            val map = mapView.map
            map.setOnMapClickListener { onMapClick() }
            map.setOnMapLongClickListener { 
                mapView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                onMapLongClick(it.latitude, it.longitude) 
            }
            map.setOnMarkerClickListener { marker ->
                val spot = marker.`object` as? FishingSpot
                if (spot != null) { onMarkerClick(spot); true } else false
            }
            aMap = map
            mapView
        },
        modifier = modifier,
        update = {
            aMap?.clear()
            // 绘制已保存的钓点
            spots.forEach { spot ->
                val bitmap = createBalloonMarkerBitmap(context, spot.iconColor)
                val marker = aMap?.addMarker(
                    MarkerOptions()
                        .position(LatLng(spot.latitude, spot.longitude))
                        .title(spot.name)
                        .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                        .anchor(0.5f, 104f / 120f)
                )
                marker?.`object` = spot
            }

            // 绘制长按选定中准备标记的临时钓点 (橙黄亮色)
            pendingLocation?.let { (lat, lon) ->
                val bitmap = createBalloonMarkerBitmap(context, "#FFA726")
                aMap?.addMarker(
                    MarkerOptions()
                        .position(LatLng(lat, lon))
                        .title("准备标记点")
                        .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                        .anchor(0.5f, 104f / 120f)
                )
            }
        }
    )
}
