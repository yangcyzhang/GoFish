package com.yangcy.gofish.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.TextureMapView
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.MyLocationStyle
import com.yangcy.gofish.data.model.FishingSpot
import com.yangcy.gofish.ui.viewmodel.FishViewModel
import com.yangcy.gofish.util.AnalyticsManager
import com.umeng.analytics.MobclickAgent
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
/**
 * 辅助函数：调整地图图标的大小
 */
/**
 * 辅助函数：调整地图图标的大小，支持矢量图并绘制在 Canvas 上
 */
private fun getResizedBitmapDescriptor(
    context: android.content.Context,
    resourceId: Int,
    width: Int,
    height: Int,
    isMyLocation: Boolean = false
): com.amap.api.maps.model.BitmapDescriptor {
    val drawable = ContextCompat.getDrawable(context, resourceId)
    val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(resultBitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

    if (isMyLocation) {
        // 模仿图片 2 的逻辑，但使用与标记点一致的“水滴”形状
        val centerX = width / 2f
        val centerY = height * 0.38f // 圆心上移，使收尖位置更高
        val radius = width * 0.38f
        val tipY = height * 0.88f
        
        // 1. 绘制带有阴影的白色外描边 (水滴形)
        paint.color = android.graphics.Color.WHITE
        paint.setShadowLayer(12f, 0f, 4f, android.graphics.Color.argb(80, 0, 0, 0))
        drawTeardropPath(canvas, centerX, centerY, radius, tipY, paint)
        paint.clearShadowLayer()

        // 2. 绘制内部主体 (背景设为纯白色，使中心内容更突出)
        paint.color = android.graphics.Color.WHITE
        val colorRadius = radius * 0.92f
        drawTeardropPath(canvas, centerX, centerY, colorRadius, tipY, paint)
        
        val themeColor = android.graphics.Color.parseColor("#00ADB5")

        // 3. 绘制带有弧形底边的指向箭头 (与 Logo 圆圈对称)
        val arrowPath = android.graphics.Path()
        val arrowTipY = tipY - 10f
        val arrowBaseY = centerY + colorRadius * 0.75f // 随着圆心上移，稍微调整箭头起始位置
        val arrowWidth = width * 0.25f
        
        arrowPath.moveTo(centerX, arrowTipY)
        // 连线到左侧
        arrowPath.lineTo(centerX - arrowWidth / 2, arrowBaseY)
        // 绘制底部弧线 (减小弧度：通过增加矩形高度和减小扫过的角度或使用较平坦的矩形)
        val arcRect = android.graphics.RectF(centerX - arrowWidth / 2, arrowBaseY - 5f, centerX + arrowWidth / 2, arrowBaseY + 5f)
        arrowPath.arcTo(arcRect, 180f, -180f, false)
        // 连线回到尖端
        arrowPath.lineTo(centerX, arrowTipY)
        arrowPath.close()
        
        paint.color = themeColor
        paint.style = android.graphics.Paint.Style.FILL
        canvas.drawPath(arrowPath, paint)
        
        // 4. 绘制带有圆圈包裹的 Logo 区域
        val logoContainerRadius = colorRadius * 0.6f
        
        // 绘制 Logo 的背景色 (使用极浅的主题色调，区别于主题色本身)
        paint.color = android.graphics.Color.parseColor("#E0F7FA")
        paint.style = android.graphics.Paint.Style.FILL
        canvas.drawCircle(centerX, centerY, logoContainerRadius, paint)

        // 绘制 Logo 的圆形边框
        paint.color = themeColor
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawCircle(centerX, centerY, logoContainerRadius, paint)
        
        canvas.save()
        val clipPath = android.graphics.Path()
        clipPath.addCircle(centerX, centerY, logoContainerRadius - 2f, android.graphics.Path.Direction.CW)
        canvas.clipPath(clipPath)
        
        drawable?.let {
            it.setTint(themeColor)
            val iconSize = (logoContainerRadius * 1.4f).toInt()
            it.setBounds(
                (centerX - iconSize / 2).toInt(),
                (centerY - iconSize / 2).toInt(),
                (centerX + iconSize / 2).toInt(),
                (centerY + iconSize / 2).toInt()
            )
            it.draw(canvas)
        }
        canvas.restore()
    } else {
        // 原有逻辑 fallback
        val bitmap = if (drawable is BitmapDrawable) {
            drawable.bitmap
        } else if (drawable != null) {
            val b = Bitmap.createBitmap(
                drawable.intrinsicWidth.takeIf { it > 0 } ?: width,
                drawable.intrinsicHeight.takeIf { it > 0 } ?: height,
                Bitmap.Config.ARGB_8888
            )
            val c = Canvas(b)
            drawable.setBounds(0, 0, c.width, c.height)
            drawable.draw(c)
            b
        } else {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
        return BitmapDescriptorFactory.fromBitmap(scaledBitmap)
    }
    
    return BitmapDescriptorFactory.fromBitmap(resultBitmap)
}

/**
 * 辅助函数：根据颜色 Hue 生成模仿高德地图样式的标记点 (图片 1)
 * 使用内置的 ImageVector 绘制，确保形状与系统图标一致
 */
private fun getResizedDefaultMarker(
    context: android.content.Context,
    width: Int,
    height: Int,
    colorHex: String = "#00ADB5"
): com.amap.api.maps.model.BitmapDescriptor {
    val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(resultBitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

    // 我们使用自定义绘制来精确控制颜色、白边和中心圆点，但基于标准的 LocationOn 比例
    val centerX = width / 2f
    val centerY = height * 0.4f
    val whiteRadius = width * 0.42f
    val tipY = height * 0.88f

    // 1. 绘制阴影和白色背景
    paint.color = android.graphics.Color.WHITE
    paint.setShadowLayer(8f, 0f, 3f, android.graphics.Color.argb(70, 0, 0, 0))
    drawTeardropPath(canvas, centerX, centerY, whiteRadius, tipY, paint)
    paint.clearShadowLayer()

    // 2. 绘制内部颜色
    paint.color = android.graphics.Color.parseColor(colorHex)
    val colorRadius = whiteRadius * 0.92f // 大幅增加比例，使间距最小化
    drawTeardropPath(canvas, centerX, centerY, colorRadius, tipY, paint)
    
    // 3. 绘制中间的白色圆点
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(centerX, centerY, colorRadius * 0.35f, paint)
    
    return BitmapDescriptorFactory.fromBitmap(resultBitmap)
}

private fun drawTeardropPath(canvas: Canvas, centerX: Float, centerY: Float, radius: Float, tipY: Float, paint: android.graphics.Paint) {
    val path = android.graphics.Path()
    
    // 算法优化：使用切线原理确保圆弧与尖角的过渡绝对平滑
    val distance = tipY - centerY
    if (distance <= radius) {
        // 如果尖端在圆内，退化为圆形
        canvas.drawCircle(centerX, centerY, radius, paint)
        return
    }
    
    // 计算切点角度
    // sin(theta) = radius / distance
    val theta = Math.asin((radius / distance).toDouble()).toFloat()
    val thetaDeg = Math.toDegrees(theta.toDouble()).toFloat()
    
    // 圆弧部分：从 (90 + theta) 度开始，绕一圈到 (90 - theta) 度
    // 这样在结束点连线到 tipY 时，连线正好是圆的切线
    path.arcTo(
        centerX - radius, centerY - radius, centerX + radius, centerY + radius,
        90f + thetaDeg, 360f - 2 * thetaDeg, false
    )
    
    // 连线到尖端
    path.lineTo(centerX, tipY)
    path.close()

    canvas.drawPath(path, paint)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FishingMapScreen(viewModel: FishViewModel) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val clipboardManager = LocalClipboardManager.current

    // Debounce state
    var lastClickTime by remember { mutableLongStateOf(0L) }
    fun isClickAllowed(): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime > 500L) {
            lastClickTime = currentTime
            return true
        }
        return false
    }

    // SharedPrefs for Map State Persistence
    val sharedPrefs = remember { context.getSharedPreferences("map_prefs", android.content.Context.MODE_PRIVATE) }
    
    // AMap Instance using TextureMapView (more stable on emulators)
    val mapView = remember { TextureMapView(context) }
    var aMap by remember { mutableStateOf<AMap?>(null) }
    
    // Database State
    val spots by viewModel.fishingSpots.collectAsState()
    val selectedLocation by viewModel.selectedLocation.collectAsState()

    // User Selection / Interactive states
    var selectedSpot by remember { mutableStateOf<FishingSpot?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }
    var showSpotList by remember { mutableStateOf(false) }
    var selectedMapType by remember { mutableIntStateOf(AMap.MAP_TYPE_NORMAL) }
    var activeCategory by remember { mutableStateOf("全部") }

    // Modals
    var showAddSpotDialog by remember { mutableStateOf(false) }
    var spotToDelete by remember { mutableStateOf<FishingSpot?>(null) }
    var showEditSpotDialog by remember { mutableStateOf(false) }
    var showShareDialogSpot by remember { mutableStateOf<FishingSpot?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var selectedTempCoords by remember { mutableStateOf<LatLng?>(null) }

    // Markers management
    val markerMap = remember { mutableMapOf<Int, Marker>() }


    // Lifecycle handling for MapView
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Sync Location from ViewModel to AMap only on initial load or manual trigger
    var initialCameraSet by remember { mutableStateOf(false) }
    LaunchedEffect(aMap) {
        val map = aMap ?: return@LaunchedEffect
        if (!initialCameraSet) {
            // Restore last camera position or use default
            val lastLat = sharedPrefs.getFloat("last_lat", selectedLocation.lat.toFloat()).toDouble()
            val lastLon = sharedPrefs.getFloat("last_lon", selectedLocation.lon.toFloat()).toDouble()
            val lastZoom = sharedPrefs.getFloat("last_zoom", 15f)
            
            val target = LatLng(lastLat, lastLon)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(target, lastZoom))
            initialCameraSet = true
        }
    }

    // Filter and Update Markers
    val filteredSpots = remember(spots, activeCategory) {
        if (activeCategory == "全部") spots
        else spots.filter { it.fishSpecies.contains(activeCategory) || it.fee.contains(activeCategory) || it.notes.contains(activeCategory) }
    }

    LaunchedEffect(filteredSpots, aMap, selectedTempCoords) {
        val map = aMap ?: return@LaunchedEffect
        
        // Clear all except potentially the My Location icon if handled internally
        map.clear() 
        markerMap.clear()
        
        // Re-enable My Location style
        val myLocationStyle = MyLocationStyle()
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
        myLocationStyle.showMyLocation(true)
        // 降低精度圆圈的透明度，使其更隐蔽 (argb(20, ...))
        myLocationStyle.radiusFillColor(android.graphics.Color.argb(20, 30, 136, 229))
        myLocationStyle.strokeColor(android.graphics.Color.argb(40, 30, 136, 229))
        myLocationStyle.strokeWidth(1f)
        
        // 调大定位蓝点图标 (140x140)，使其更明显
        myLocationStyle.myLocationIcon(getResizedBitmapDescriptor(context, com.yangcy.gofish.R.drawable.ic_launcher_foreground, 140, 140, isMyLocation = true))
        map.myLocationStyle = myLocationStyle
        map.isMyLocationEnabled = true

        // 1. Add Filtered Database Markers
        filteredSpots.forEach { spot ->
            val pos = LatLng(spot.latitude, spot.longitude)
            val markerOptions = MarkerOptions()
                .position(pos)
                .title(spot.name)
                .snippet(spot.address)
                .icon(getResizedDefaultMarker(
                    context = context,
                    width = 80, 
                    height = 100,
                    colorHex = spot.iconColor
                ))
            val marker = map.addMarker(markerOptions)
            marker.`object` = spot
            markerMap[spot.id] = marker
        }

        // 2. Add or Update the Interactive Selection Pointer (Temp Marker)
        selectedTempCoords?.let { coords ->
            val markerOptions = MarkerOptions()
                .position(coords)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                .setFlat(false)
                .infoWindowEnable(false)
            
            val marker = map.addMarker(markerOptions)
            // Use drop animation for the selection feeling
            marker.setAnimation(com.amap.api.maps.model.animation.AnimationSet(true).apply {
                addAnimation(com.amap.api.maps.model.animation.AlphaAnimation(0f, 1f).apply { setDuration(300) })
            })
            marker.startAnimation()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        
        // ------------------ NATIVE AMAP VIEW ------------------
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                if (aMap == null) {
                    val map = view.map
                    // Configure AMap
                    map.uiSettings.apply {
                        isZoomControlsEnabled = false
                        isCompassEnabled = true
                        isScaleControlsEnabled = true
                        isMyLocationButtonEnabled = false
                    }
                    
                    // My Location Style (Arrow style, but don't force follow camera)
                    val myLocationStyle = MyLocationStyle()
                    myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
                    myLocationStyle.showMyLocation(true)
                    map.myLocationStyle = myLocationStyle
                    map.isMyLocationEnabled = true
                    
                    // Listeners
                    map.setOnCameraChangeListener(object : AMap.OnCameraChangeListener {
                        override fun onCameraChange(p0: com.amap.api.maps.model.CameraPosition?) {}
                        override fun onCameraChangeFinish(pos: com.amap.api.maps.model.CameraPosition?) {
                            pos?.let {
                                sharedPrefs.edit()
                                    .putFloat("last_lat", it.target.latitude.toFloat())
                                    .putFloat("last_lon", it.target.longitude.toFloat())
                                    .putFloat("last_zoom", it.zoom)
                                    .apply()
                            }
                        }
                    })

                    map.setOnMapClickListener {
                        focusManager.clearFocus()
                        isSearchFocused = false
                        selectedTempCoords = null
                        selectedSpot = null
                        showSpotList = false
                    }
                    
                    map.setOnMapLongClickListener { latLng ->
                        AnalyticsManager.trackEvent(context, AnalyticsManager.EVENT_MAP_LONG_PRESS)
                        Toast.makeText(context, "已选定位置，请在底部确认标记", Toast.LENGTH_SHORT).show()
                        focusManager.clearFocus()
                        isSearchFocused = false
                        selectedTempCoords = latLng
                        selectedSpot = null
                        showSpotList = false
                        
                        map.animateCamera(CameraUpdateFactory.newLatLng(latLng))
                    }
                    
                    map.setOnMarkerClickListener { marker ->
                        val spot = marker.`object` as? FishingSpot
                        if (spot != null) {
                            selectedSpot = spot
                            selectedTempCoords = null
                            showSpotList = false
                            true // consume event
                        } else false
                    }
                    
                    aMap = map
                }
            }
        )

        // ------------------ TOP SEARCH & ACTION BAR ------------------
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp)
                .align(Alignment.TopStart), // Align to START (Left)
            horizontalAlignment = Alignment.Start // Left-aligned column
        ) {
            val searchBarWidth by animateFloatAsState(targetValue = if (isSearchFocused) 1f else 0.5f)
            
            Row(
                modifier = Modifier
                    .fillMaxWidth(searchBarWidth)
                    .height(56.dp) // Fixed height
                    .padding(horizontal = 16.dp)
                    .shadow(8.dp, RoundedCornerShape(28.dp)) // Pill shape
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), RoundedCornerShape(28.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(28.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (isClickAllowed()) showSpotList = !showSpotList },
                    modifier = Modifier.testTag("toggle_spot_list_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = "钓点列表",
                        tint = if (showSpotList) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                TextField(
                    value = searchQuery,
                    onValueChange = { query ->
                        searchQuery = query
                        // Local search logic: only search within existing saved spots
                        val matchedSpot = spots.firstOrNull { it.name.contains(query, ignoreCase = true) }
                        if (matchedSpot != null) {
                            aMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(matchedSpot.latitude, matchedSpot.longitude), 15f))
                        }
                    },
                    placeholder = { Text("搜索我的钓点...", fontSize = 14.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { isSearchFocused = it.isFocused }
                        .testTag("map_search_input")
                )
                
                if (isSearchFocused || searchQuery.isNotEmpty()) {
                    IconButton(onClick = { 
                        searchQuery = ""
                        isSearchFocused = false
                        focusManager.clearFocus()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(20.dp))
                    }
                }

                IconButton(
                    onClick = {
                        if (isClickAllowed()) {
                            if (searchQuery.trim().isNotEmpty()) {
                                val matchedSpot = spots.firstOrNull { it.name.contains(searchQuery, ignoreCase = true) }
                                if (matchedSpot != null) {
                                    focusManager.clearFocus()
                                    isSearchFocused = false
                                    aMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(matchedSpot.latitude, matchedSpot.longitude), 16f))
                                } else {
                                    Toast.makeText(context, "未找到名为『$searchQuery』的钓点", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                // If empty, just focus or toggle
                                isSearchFocused = true
                            }
                        }
                    }
                ) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Category Filter Chips
            AnimatedVisibility(
                visible = !isSearchFocused,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val categories = listOf("全部", "免费", "路亚", "黑坑", "斤塘", "野钓", "水库")
                    items(categories) { cat ->
                        val isSelected = activeCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { 
                                activeCategory = cat 
                                focusManager.clearFocus()
                            },
                            label = { Text(cat, fontSize = 12.sp, fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White,
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                selectedBorderColor = Color.Transparent,
                                borderWidth = 1.dp,
                                selectedBorderWidth = 0.dp
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }
        }

        // ------------------ ON-MAP OVERLAYS (CONTROLS COLUMN) ------------------
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 120.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Map Type Toggle Button (Amap Style: Quick Switch)
            Card(
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.size(44.dp).clickable {
                    if (isClickAllowed()) {
                        selectedMapType = when (selectedMapType) {
                            AMap.MAP_TYPE_NORMAL -> AMap.MAP_TYPE_SATELLITE
                            else -> AMap.MAP_TYPE_NORMAL
                        }
                        aMap?.mapType = selectedMapType
                    }
                }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = if (selectedMapType == AMap.MAP_TYPE_SATELLITE) Icons.Default.Info else Icons.Default.Settings,
                        contentDescription = "Map Type",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (selectedMapType == AMap.MAP_TYPE_SATELLITE) "标" else "卫",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp)
                    )
                }
            }

            // Zoom Controls
            Card(
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { aMap?.animateCamera(CameraUpdateFactory.zoomIn()) }) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    HorizontalDivider(modifier = Modifier.width(20.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    IconButton(onClick = { aMap?.animateCamera(CameraUpdateFactory.zoomOut()) }) {
                        // Using a custom dash/minus line instead of Icons.Default.Clear
                        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier.size(12.dp, 2.dp).background(MaterialTheme.colorScheme.onSurface))
                        }
                    }
                }
            }

            // My Location Button
            FloatingActionButton(
                onClick = {
                    if (isClickAllowed()) {
                        viewModel.triggerPreciseLocation(context, showToast = false)
                        // Force camera update on manual click
                        val target = LatLng(selectedLocation.lat, selectedLocation.lon)
                        aMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 15f))
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(54.dp).testTag("gps_center_button")
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "My Location",
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // ------------------ BOTTOM INTERACTIVE PANEL (DYNAMIC) ------------------
        // Case A: Temporary crosshair active -> Show "Add Spot here" panel
        selectedTempCoords?.let { coords ->
            val formattedLon = String.format(Locale.CHINA, "%.4f", coords.longitude)
            val formattedLat = String.format(Locale.CHINA, "%.4f", coords.latitude)

            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .shadow(12.dp, RoundedCornerShape(20.dp))
                    .testTag("temp_crosshair_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "选定钓点经纬度坐标", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "经度: ${formattedLon}°E   纬度: ${formattedLat}°N", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { selectedTempCoords = null }, modifier = Modifier.weight(1f)) { Text("取消选择") }
                        Button(
                            onClick = { showAddSpotDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1.5f).testTag("add_spot_at_coords")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("标记并记录钓点", fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Case B: Existing spot selected -> Show details in ModalBottomSheet
        if (selectedSpot != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedSpot = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .width(36.dp)
                            .height(5.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), CircleShape)
                    )
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                selectedSpot?.let { spot ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .padding(bottom = 32.dp) // Extra padding for system bars
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(Color(android.graphics.Color.parseColor(spot.iconColor)).copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                Icon(imageVector = getIconByStyle(spot.iconStyle), contentDescription = null, tint = Color(android.graphics.Color.parseColor(spot.iconColor)), modifier = Modifier.size(32.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = spot.name, fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                                Text(text = spot.address, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DetailInfoChip(Icons.Default.Star, spot.fee, Color(0xFFE8F5E9), Color(0xFF2E7D32))
                            DetailInfoChip(Icons.Default.Face, spot.fishSpecies.take(10), Color(0xFFE3F2FD), Color(0xFF1565C0))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        if (!spot.imageUrl.isNullOrEmpty()) {
                            AsyncImage(model = spot.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(16.dp)))
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, null, tint = Color(0xFFFFA000), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("实战技巧/备注", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = spot.notes.ifEmpty { "暂无备注" }, fontSize = 14.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { 
                                    if (isClickAllowed()) {
                                        AnalyticsManager.trackEvent(context, AnalyticsManager.EVENT_LAUNCH_NAVI, mapOf("spot_name" to spot.name))
                                        launchAMapNavi(context, spot.latitude, spot.longitude)
                                    }
                                }, 
                                modifier = Modifier.weight(1.5f).height(48.dp), 
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("高德导航", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(onClick = { showEditSpotDialog = true }, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)) { Text("编辑") }
                        }
                    }
                }
            }
        }

        // ------------------ FLOATING SPOTS LIST DRAWER ------------------
        AnimatedVisibility(
            visible = showSpotList,
            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(top = 90.dp, start = 16.dp, end = 16.dp, bottom = 100.dp).widthIn(max = 360.dp).fillMaxHeight()
        ) {
            Card(modifier = Modifier.fillMaxSize().shadow(16.dp, RoundedCornerShape(24.dp)).border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(24.dp)), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.List, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "我的钓点库 (${spots.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        TextButton(onClick = { showImportDialog = true }) { Icon(Icons.Default.AddCircle, null, modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("导入口令", fontSize = 12.sp) }
                        IconButton(onClick = { showSpotList = false }) { Icon(Icons.Default.Close, null, modifier = Modifier.size(20.dp)) }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (spots.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                                Text(text = "暂无标记过的钓点\n在地图上长按即可添加！", fontSize = 13.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                            items(spots) { spot ->
                                Card(modifier = Modifier.fillMaxWidth().clickable {
                                    selectedSpot = spot
                                    selectedTempCoords = null
                                    showSpotList = false // Hide drawer after selection
                                    aMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(spot.latitude, spot.longitude), 15f))
                                }, colors = CardDefaults.cardColors(containerColor = if (selectedSpot?.id == spot.id) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)), shape = RoundedCornerShape(12.dp)) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(spot.iconColor))))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(text = spot.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                            
                                            // Action Row for Spot
                                            Row {
                                                IconButton(
                    onClick = { 
                        if (isClickAllowed()) {
                            AnalyticsManager.trackEvent(context, AnalyticsManager.EVENT_SHARE_SPOT, mapOf("name" to spot.name))
                            showShareDialogSpot = spot 
                        }
                    }, 
                    modifier = Modifier.size(24.dp)
                ) { Icon(Icons.Default.Share, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) }
                                                Spacer(modifier = Modifier.width(4.dp))
                                                IconButton(onClick = { 
                                                    if (isClickAllowed()) {
                                                        spotToDelete = spot
                                                    }
                                                }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(16.dp)) }
                                            }
                                        }
                                        Text(text = spot.address, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS (REUSED FROM PREVIOUS LOGIC) ---
    if (showAddSpotDialog && selectedTempCoords != null) {
        val presetNames = listOf("我的钓点", "河道水域", "秘密基地", "桥墩下", "水库边", "回水湾")
        var spotName by remember { mutableStateOf(presetNames[0]) }
        var customName by remember { mutableStateOf("") }
        var isCustomName by remember { mutableStateOf(false) }
        
        // Dynamic address lookup for the selected coordinates
        var spotAddress by remember { mutableStateOf("获取地址中...") }
        LaunchedEffect(selectedTempCoords) {
            val geocoder = android.location.Geocoder(context, Locale.CHINA)
            try {
                val addresses = geocoder.getFromLocation(selectedTempCoords!!.latitude, selectedTempCoords!!.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    spotAddress = addresses[0].getAddressLine(0) ?: "未知地点"
                } else {
                    spotAddress = "野外钓点"
                }
            } catch (e: Exception) {
                spotAddress = "坐标：${String.format(Locale.CHINA, "%.4f, %.4f", selectedTempCoords!!.longitude, selectedTempCoords!!.latitude)}"
            }
        }
        
        var selectedColor by remember { mutableStateOf("#00ADB5") }
        var selectedStyle by remember { mutableStateOf("pin") }
        var fishSpecies by remember { mutableStateOf("") }
        var baitUsed by remember { mutableStateOf("") }
        var spotNotes by remember { mutableStateOf("") }
        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
        var arrivalTime by remember { mutableStateOf(timeFormat.format(Date())) }

        AlertDialog(
            onDismissRequest = { showAddSpotDialog = false },
            title = { Text("标记新钓点", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("选择钓点名称:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    ScrollableRowSelector(
                        items = presetNames + "自定义",
                        selectedItem = if (isCustomName) "自定义" else spotName,
                        onSelect = { 
                            if (it == "自定义") isCustomName = true
                            else {
                                isCustomName = false
                                spotName = it
                            }
                        }
                    )

                    if (isCustomName) {
                        OutlinedTextField(value = customName, onValueChange = { customName = it }, label = { Text("输入自定义名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }

                    OutlinedTextField(value = spotAddress, onValueChange = { spotAddress = it }, label = { Text("详细地址 (已自动获取)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    
                    Text("标记颜色:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("#00ADB5", "#FF5252", "#4CAF50", "#FFC107", "#9C27B0").forEach { color ->
                            Box(modifier = Modifier.size(30.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(color))).border(if(selectedColor == color) 2.dp else 0.dp, Color.Black, CircleShape).clickable { selectedColor = color })
                        }
                    }
                    OutlinedTextField(value = fishSpecies, onValueChange = { fishSpecies = it }, label = { Text("目标鱼种 (必填)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = spotNotes, onValueChange = { spotNotes = it }, label = { Text("备注/心得") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val finalName = if (isCustomName) customName.trim() else spotName
                    val missingFields = mutableListOf<String>()
                    if (finalName.isEmpty()) missingFields.add("名称")
                    if (fishSpecies.trim().isEmpty()) missingFields.add("鱼种")
                    
                    if (missingFields.isNotEmpty()) {
                        Toast.makeText(context, "请填写：${missingFields.joinToString("、")}", Toast.LENGTH_SHORT).show()
                    } else {
                        AnalyticsManager.trackEvent(context, AnalyticsManager.EVENT_ADD_SPOT, mapOf("name" to finalName))
                        viewModel.addFishingSpot(FishingSpot(
                            name = finalName, 
                            address = spotAddress, 
                            latitude = selectedTempCoords!!.latitude, 
                            longitude = selectedTempCoords!!.longitude, 
                            iconColor = selectedColor, 
                            iconStyle = selectedStyle, 
                            arrivalTime = arrivalTime, 
                            fishSpecies = fishSpecies, 
                            bait = baitUsed, 
                            fee = "", // Fee field repurposed or removed
                            notes = spotNotes
                        ))
                        selectedTempCoords = null
                        showAddSpotDialog = false
                        Toast.makeText(context, "钓点标记成功！", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showAddSpotDialog = false }) { Text("取消") } }
        )
    }

    // Delete Confirmation Dialog
    if (spotToDelete != null) {
        AlertDialog(
            onDismissRequest = { spotToDelete = null },
            title = { Text("确认删除", fontWeight = FontWeight.Bold) },
            text = { Text("确定要永久删除钓点『${spotToDelete?.name}』吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        spotToDelete?.let {
                            AnalyticsManager.trackEvent(context, AnalyticsManager.EVENT_DELETE_SPOT, mapOf("name" to it.name))
                            viewModel.deleteFishingSpot(it)
                            if (selectedSpot?.id == it.id) selectedSpot = null
                        }
                        spotToDelete = null
                        Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("确认删除") }
            },
            dismissButton = { TextButton(onClick = { spotToDelete = null }) { Text("取消") } }
        )
    }
    
    // ------------------ MODAL B: EDIT SPOT DIALOG ------------------
    if (showEditSpotDialog && selectedSpot != null) {
        val spot = selectedSpot!!
        var spotName by remember { mutableStateOf(spot.name) }
        var spotAddress by remember { mutableStateOf(spot.address) }
        var selectedColor by remember { mutableStateOf(spot.iconColor) }
        var selectedStyle by remember { mutableStateOf(spot.iconStyle) }
        var fishSpecies by remember { mutableStateOf(spot.fishSpecies) }
        var feeType by remember { mutableStateOf(spot.fee) }
        var spotNotes by remember { mutableStateOf(spot.notes) }

        AlertDialog(
            onDismissRequest = { showEditSpotDialog = false },
            title = { Text("编辑钓点详情", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = spotName, onValueChange = { spotName = it }, label = { Text("钓点名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = spotAddress, onValueChange = { spotAddress = it }, label = { Text("详细地址") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("标记颜色:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("#00ADB5", "#FF5252", "#4CAF50", "#FFC107", "#9C27B0").forEach { color ->
                            Box(modifier = Modifier.size(30.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(color))).border(if(selectedColor == color) 2.dp else 0.dp, MaterialTheme.colorScheme.primary, CircleShape).clickable { selectedColor = color })
                        }
                    }
                    OutlinedTextField(value = fishSpecies, onValueChange = { fishSpecies = it }, label = { Text("目标鱼种") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = feeType, onValueChange = { feeType = it }, label = { Text("收费标准") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = spotNotes, onValueChange = { spotNotes = it }, label = { Text("详细备注") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (spotName.trim().isNotEmpty()) {
                        val updated = spot.copy(name = spotName, address = spotAddress, iconColor = selectedColor, iconStyle = selectedStyle, fishSpecies = fishSpecies, fee = feeType, notes = spotNotes)
                        viewModel.addFishingSpot(updated)
                        selectedSpot = updated
                        showEditSpotDialog = false
                        Toast.makeText(context, "修改已保存", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("保存修改") }
            },
            dismissButton = { TextButton(onClick = { showEditSpotDialog = false }) { Text("取消") } }
        )
    }

    // ------------------ MODAL C: IMPORT SHARE CODE DIALOG ------------------
    if (showImportDialog) {
        var pastedText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("导入分享钓点口令", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "粘贴好友发送给您的『爆护口令』全文本，即可一键同步该钓点。", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(value = pastedText, onValueChange = { pastedText = it }, placeholder = { Text("在此粘贴口令...") }, modifier = Modifier.fillMaxWidth().height(120.dp), maxLines = 5)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val imported = parseShareCode(pastedText)
                    if (imported != null) {
                        AnalyticsManager.trackEvent(context, AnalyticsManager.EVENT_IMPORT_SPOT)
                        viewModel.addFishingSpot(imported)
                        showImportDialog = false
                        Toast.makeText(context, "成功导入：${imported.name}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "未能解析该口令，请确保复制了完整文本", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("开始导入") }
            },
            dismissButton = { TextButton(onClick = { showImportDialog = false }) { Text("取消") } }
        )
    }

    // ------------------ MODAL D: SHARE SPOT DIALOG ------------------
    if (showShareDialogSpot != null) {
        val spot = showShareDialogSpot!!
        val shareCodeText = generateShareCode(spot)

        AlertDialog(
            onDismissRequest = { showShareDialogSpot = null },
            title = { Text("分享钓点: ${spot.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "复制下方的【爆护口令】发送给好友，对方打开 App 即可自动导入。", fontSize = 13.sp)
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                        Text(text = shareCodeText, modifier = Modifier.padding(12.dp), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(shareCodeText))
                    Toast.makeText(context, "口令已复制到剪贴板！", Toast.LENGTH_SHORT).show()
                    showShareDialogSpot = null
                }) { Text("复制口令并关闭") }
            }
        )
    }
}

@Composable
private fun DetailInfoChip(icon: ImageVector, label: String, containerColor: Color, contentColor: Color) {
    Surface(color = containerColor, shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, color = contentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun generateShareCode(spot: FishingSpot): String {
    return """
        【爆护钓点分享】
        名称：${spot.name}
        坐标：${spot.latitude},${spot.longitude}
        主攻鱼种：${spot.fishSpecies}
        费用情况：${spot.fee}
        作钓备注：${spot.notes}
        --- 复制此段全部文字，打开爆护 App 即可一键导入钓点！ ---
    """.trimIndent()
}

private fun getIconByStyle(style: String): ImageVector {
    return when (style) {
        "fish" -> Icons.Default.Star
        "anchor" -> Icons.Default.Build
        "flag" -> Icons.Default.Favorite
        "star" -> Icons.Default.FavoriteBorder
        else -> Icons.Default.Place
    }
}

private fun parseShareCode(text: String): FishingSpot? {
    try {
        val name = Regex("名称[：:](.*)").find(text)?.groupValues?.get(1)?.trim() ?: ""
        val coords = Regex("坐标[：:]([-0-9.]+)\\s*,\\s*([-0-9.]+)").find(text) ?: return null
        val lat = coords.groupValues[1].toDoubleOrNull() ?: return null
        val lng = coords.groupValues[2].toDoubleOrNull() ?: return null
        return FishingSpot(name = name, address = "分享坐标", latitude = lat, longitude = lng, iconColor = "#FF5722", iconStyle = "star", arrivalTime = "", fishSpecies = "", bait = "", fee = "", notes = "")
    } catch (e: Exception) { return null }
}

private fun launchAMapNavi(context: android.content.Context, lat: Double, lon: Double) {
    try {
        val uri = "androidamap://navi?sourceApplication=BaoHu&lat=$lat&lon=$lon&dev=0&style=2"
        val intent = android.content.Intent("android.intent.action.VIEW", android.net.Uri.parse(uri))
        intent.setPackage("com.autonavi.minimap")
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "未检测到高德地图，请先安装", Toast.LENGTH_LONG).show()
    }
}
