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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.TextureMapView
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.MyLocationStyle
import com.yangcy.gofish.data.model.FishingSpot
import com.yangcy.gofish.ui.viewmodel.FishViewModel
import com.yangcy.gofish.util.analytics
import com.yangcy.gofish.util.showToast
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun FishingMapScreen(viewModel: FishViewModel) {
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
    val isPositioning by viewModel.isPositioning.collectAsState()

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
    var viewDocType by remember { mutableStateOf<DocType?>(null) }
    var detectedImportSpot by remember { mutableStateOf<FishingSpot?>(null) }
    var lastProcessedClipText by remember { 
        mutableStateOf(sharedPrefs.getString("last_processed_clip", "") ?: "") 
    }

    // Markers management
    val markerMap = remember { mutableMapOf<Int, Marker>() }

    // 优化：监听窗口焦点变化来识别剪贴板（Android 12+ 必须获得焦点后才能读取）
    val windowInfo = androidx.compose.ui.platform.LocalWindowInfo.current
    LaunchedEffect(windowInfo.isWindowFocused) {
        if (windowInfo.isWindowFocused) {
            try {
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                if (clipboard.hasPrimaryClip()) {
                    val clipData = clipboard.primaryClip
                    if (clipData != null && clipData.itemCount > 0) {
                        val text = clipData.getItemAt(0).text?.toString() ?: ""
                        
                        // 核心：如果当前剪贴板内容与上次处理过的内容一致，则不再重复识别
                        if (text == lastProcessedClipText) return@LaunchedEffect
                        
                        if (text.contains("【爆护钓点分享】")) {
                            val spot = parseShareCode(text)
                            if (spot != null) {
                                lastProcessedClipText = text
                                sharedPrefs.edit().putString("last_processed_clip", text).apply()
                                detectedImportSpot = spot
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FishingMapScreen", "Clipboard recognition error", e)
            }
        }
    }

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
                        analytics.trackEvent("map_long_press")
                        showToast("已选定位置，请在底部确认标记")
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
                                    showToast("已为您定位至『${matchedSpot.name}』")
                                    aMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(matchedSpot.latitude, matchedSpot.longitude), 16f))
                                    searchQuery = "" // 搜索成功后清空输入框
                                } else {
                                    showToast("未找到名为『$searchQuery』的钓点")
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
                    val categories = listOf("全部", "手竿", "路亚", "海竿", "黑坑", "野钓", "水库")
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
            // About/Privacy Button
            Card(
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.size(44.dp).clickable {
                    if (isClickAllowed()) {
                        viewDocType = DocType.PRIVACY_POLICY
                    }
                }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "关于与隐私",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

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
                    if (isClickAllowed() && !isPositioning) {
                        viewModel.triggerPreciseLocation(onFrequentRequest = { showToast("请求过于频繁，请稍后再试") }, onResult = { name -> showToast("成功获取精准位置：$name") })
                        // Force camera update on manual click
                        val target = LatLng(selectedLocation.lat, selectedLocation.lon)
                        aMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 15f))
                    }
                },
                containerColor = if (isPositioning) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                contentColor = if (isPositioning) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                shape = CircleShape,
                modifier = Modifier.size(54.dp).testTag("gps_center_button")
            ) {
                if (isPositioning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "My Location",
                        modifier = Modifier.size(26.dp)
                    )
                }
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
                        Text(text = if(activeCategory == "全部") "我的钓点库 (${spots.size})" else "$activeCategory 钓点 (${filteredSpots.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
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
                            items(filteredSpots) { spot ->
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
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(text = spot.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    if (spot.fee.isNotEmpty()) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Surface(
                                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text(
                                                                text = spot.fee,
                                                                fontSize = 9.sp,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                                Text(text = spot.address, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                            
                                            // Action Row for Spot
                                            Row {
                                                IconButton(
                                                    onClick = { 
                                                        if (isClickAllowed()) {
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
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (viewDocType != null) {
            DocDetailView(type = viewDocType!!, onBack = { viewDocType = null })
        }
    }

    // --- DIALOGS ---
    if (showAddSpotDialog && selectedTempCoords != null) {
        val presetNames = listOf("我的钓点", "河道水域", "秘密基地", "桥墩下", "水库边", "回水湾")
        var spotName by remember { mutableStateOf(presetNames[0]) }
        var customName by remember { mutableStateOf("") }
        var isCustomName by remember { mutableStateOf(false) }
        var spotCategory by remember { mutableStateOf("手竿") }
        
        val presetFish = listOf("鲫鱼", "鲤鱼", "草鱼", "翘嘴", "鲈鱼", "黑鱼", "罗非鱼")
        var selectedFishPreset by remember { mutableStateOf(presetFish[0]) }
        var customFishName by remember { mutableStateOf("") }
        var isCustomFish by remember { mutableStateOf(false) }
        
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
                            else { isCustomName = false; spotName = it }
                        }
                    )
                    if (isCustomName) {
                        OutlinedTextField(value = customName, onValueChange = { customName = it }, label = { Text("输入自定义名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    Text("选择钓点类别:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    ScrollableRowSelector(items = listOf("手竿", "路亚", "海竿", "黑坑", "野钓", "水库"), selectedItem = spotCategory, onSelect = { spotCategory = it })
                    OutlinedTextField(value = spotAddress, onValueChange = { spotAddress = it }, label = { Text("详细地址") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    
                    Text("标记颜色:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("#00ADB5", "#FF5252", "#4CAF50", "#FFC107", "#9C27B0").forEach { color ->
                            Box(modifier = Modifier.size(30.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(color))).border(if(selectedColor == color) 2.dp else 0.dp, Color.Black, CircleShape).clickable { selectedColor = color })
                        }
                    }

                    Text("目标鱼种:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    ScrollableRowSelector(items = presetFish + "自定义", selectedItem = if (isCustomFish) "自定义" else selectedFishPreset, onSelect = { 
                        if (it == "自定义") isCustomFish = true
                        else { isCustomFish = false; selectedFishPreset = it }
                    })
                    if (isCustomFish) {
                        OutlinedTextField(value = customFishName, onValueChange = { customFishName = it }, label = { Text("输入自定义鱼种") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    OutlinedTextField(value = spotNotes, onValueChange = { spotNotes = it }, label = { Text("备注/心得") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val finalName = if (isCustomName) customName.trim() else spotName
                    val finalFish = if (isCustomFish) customFishName.trim() else selectedFishPreset
                    if (finalName.isNotEmpty() && finalFish.isNotEmpty()) {
                        viewModel.addFishingSpot(FishingSpot(name = finalName, address = spotAddress, latitude = selectedTempCoords!!.latitude, longitude = selectedTempCoords!!.longitude, iconColor = selectedColor, iconStyle = "pin", arrivalTime = arrivalTime, fishSpecies = finalFish, bait = "", fee = spotCategory, notes = spotNotes))
                        selectedTempCoords = null
                        showAddSpotDialog = false
                        showToast("钓点标记成功！")
                    } else {
                        showToast("请填写完整信息")
                    }
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showAddSpotDialog = false }) { Text("取消") } }
        )
    }

    if (spotToDelete != null) {
        AlertDialog(
            onDismissRequest = { spotToDelete = null },
            title = { Text("确认删除", fontWeight = FontWeight.Bold) },
            text = { Text("确定要永久删除钓点『${spotToDelete?.name}』吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        spotToDelete?.let { viewModel.deleteFishingSpot(it) }
                        spotToDelete = null
                        showToast("已删除")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("确认删除") }
            },
            dismissButton = { TextButton(onClick = { spotToDelete = null }) { Text("取消") } }
        )
    }

    if (showEditSpotDialog && selectedSpot != null) {
        val spot = selectedSpot!!
        var spotName by remember { mutableStateOf(spot.name) }
        var spotAddress by remember { mutableStateOf(spot.address) }
        var selectedColor by remember { mutableStateOf(spot.iconColor) }
        val presetFish = listOf("鲫鱼", "鲤鱼", "草鱼", "翘嘴", "鲈鱼", "黑鱼", "罗非鱼")
        var isCustomFish by remember { mutableStateOf(!presetFish.contains(spot.fishSpecies)) }
        var selectedFishPreset by remember { mutableStateOf(if (isCustomFish) presetFish[0] else spot.fishSpecies) }
        var customFishName by remember { mutableStateOf(if (isCustomFish) spot.fishSpecies else "") }
        var feeType by remember { mutableStateOf(spot.fee) }
        var spotNotes by remember { mutableStateOf(spot.notes) }

        AlertDialog(
            onDismissRequest = { showEditSpotDialog = false },
            title = { Text("编辑钓点详情", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = spotName, onValueChange = { spotName = it }, label = { Text("钓点名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = spotAddress, onValueChange = { spotAddress = it }, label = { Text("详细地址") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("目标鱼种:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    ScrollableRowSelector(items = presetFish + "自定义", selectedItem = if (isCustomFish) "自定义" else selectedFishPreset, onSelect = { 
                        if (it == "自定义") isCustomFish = true
                        else { isCustomFish = false; selectedFishPreset = it }
                    })
                    if (isCustomFish) {
                        OutlinedTextField(value = customFishName, onValueChange = { customFishName = it }, label = { Text("输入自定义鱼种") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    Text("钓点类别:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    ScrollableRowSelector(items = listOf("手竿", "路亚", "海竿", "黑坑", "野钓", "水库"), selectedItem = feeType, onSelect = { feeType = it })
                    OutlinedTextField(value = spotNotes, onValueChange = { spotNotes = it }, label = { Text("详细备注") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    val finalFish = if (isCustomFish) customFishName.trim() else selectedFishPreset
                    if (spotName.trim().isNotEmpty() && finalFish.isNotEmpty()) {
                        val updated = spot.copy(name = spotName, address = spotAddress, iconColor = selectedColor, fishSpecies = finalFish, fee = feeType, notes = spotNotes)
                        viewModel.addFishingSpot(updated)
                        selectedSpot = updated
                        showEditSpotDialog = false
                        showToast("修改已保存")
                    }
                }) { Text("保存修改") }
            },
            dismissButton = { TextButton(onClick = { showEditSpotDialog = false }) { Text("取消") } }
        )
    }

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
                        analytics.trackEvent("import_spot_code")
                        viewModel.addFishingSpot(imported)
                        showImportDialog = false
                        showToast("成功导入：${imported.name}")
                    } else {
                        showToast("未能解析该口令")
                    }
                }) { Text("开始导入") }
            },
            dismissButton = { TextButton(onClick = { showImportDialog = false }) { Text("取消") } }
        )
    }

    if (showShareDialogSpot != null) {
        val spot = showShareDialogSpot!!
        val shareCodeText = generateShareCode(spot)
        AlertDialog(
            onDismissRequest = { showShareDialogSpot = null },
            title = { Text("分享钓点: ${spot.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "复制下方的【爆护口令】发送给好友。", fontSize = 13.sp)
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                        Text(text = shareCodeText, modifier = Modifier.padding(12.dp), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(shareCodeText))
                    lastProcessedClipText = shareCodeText
                    sharedPrefs.edit().putString("last_processed_clip", shareCodeText).apply()
                    showToast("口令已复制到剪贴板！")
                    showShareDialogSpot = null
                }) { Text("复制口令并关闭") }
            }
        )
    }

    if (detectedImportSpot != null) {
        val spot = detectedImportSpot!!
        AlertDialog(
            onDismissRequest = { detectedImportSpot = null },
            title = { Text("识别到分享钓点", fontWeight = FontWeight.Bold) },
            text = { Text("是否导入好友分享的钓点『${spot.name}』？") },
            confirmButton = {
                Button(onClick = {
                    analytics.trackEvent("import_spot_code")
                    viewModel.addFishingSpot(spot)
                    aMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(spot.latitude, spot.longitude), 15f))
                    detectedImportSpot = null
                    showToast("成功导入：${spot.name}")
                }) { Text("确认导入") }
            },
            dismissButton = { TextButton(onClick = { detectedImportSpot = null }) { Text("忽略") } }
        )
    }
}

// Helpers
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
        val centerX = width / 2f
        val centerY = height * 0.38f
        val radius = width * 0.38f
        val tipY = height * 0.88f
        paint.color = android.graphics.Color.WHITE
        paint.setShadowLayer(12f, 0f, 4f, android.graphics.Color.argb(80, 0, 0, 0))
        drawTeardropPath(canvas, centerX, centerY, radius, tipY, paint)
        paint.clearShadowLayer()
        paint.color = android.graphics.Color.WHITE
        val colorRadius = radius * 0.92f
        drawTeardropPath(canvas, centerX, centerY, colorRadius, tipY, paint)
        val themeColor = android.graphics.Color.parseColor("#00ADB5")
        val arrowPath = android.graphics.Path()
        val arrowTipY = tipY - 10f
        val arrowBaseY = centerY + colorRadius * 0.75f
        val arrowWidth = width * 0.25f
        arrowPath.moveTo(centerX, arrowTipY)
        arrowPath.lineTo(centerX - arrowWidth / 2, arrowBaseY)
        val arcRect = android.graphics.RectF(centerX - arrowWidth / 2, arrowBaseY - 5f, centerX + arrowWidth / 2, arrowBaseY + 5f)
        arrowPath.arcTo(arcRect, 180f, -180f, false)
        arrowPath.lineTo(centerX, arrowTipY)
        arrowPath.close()
        paint.color = themeColor
        paint.style = android.graphics.Paint.Style.FILL
        canvas.drawPath(arrowPath, paint)
        val logoContainerRadius = colorRadius * 0.6f
        paint.color = android.graphics.Color.parseColor("#E0F7FA")
        paint.style = android.graphics.Paint.Style.FILL
        canvas.drawCircle(centerX, centerY, logoContainerRadius, paint)
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
            it.setBounds((centerX - iconSize / 2).toInt(), (centerY - iconSize / 2).toInt(), (centerX + iconSize / 2).toInt(), (centerY + iconSize / 2).toInt())
            it.draw(canvas)
        }
        canvas.restore()
    } else {
        val bitmap = if (drawable is BitmapDrawable) drawable.bitmap else {
            val b = Bitmap.createBitmap(drawable?.intrinsicWidth?.takeIf { it > 0 } ?: width, drawable?.intrinsicHeight?.takeIf { it > 0 } ?: height, Bitmap.Config.ARGB_8888)
            val c = Canvas(b)
            drawable?.setBounds(0, 0, b.width, b.height)
            drawable?.draw(c)
            b
        }
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888), width, height, true)
        return BitmapDescriptorFactory.fromBitmap(scaledBitmap)
    }
    return BitmapDescriptorFactory.fromBitmap(resultBitmap)
}

private fun getResizedDefaultMarker(
    context: android.content.Context,
    width: Int,
    height: Int,
    colorHex: String = "#00ADB5"
): com.amap.api.maps.model.BitmapDescriptor {
    val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(resultBitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    val centerX = width / 2f
    val centerY = height * 0.4f
    val whiteRadius = width * 0.42f
    val tipY = height * 0.88f
    paint.color = android.graphics.Color.WHITE
    paint.setShadowLayer(8f, 0f, 3f, android.graphics.Color.argb(70, 0, 0, 0))
    drawTeardropPath(canvas, centerX, centerY, whiteRadius, tipY, paint)
    paint.clearShadowLayer()
    paint.color = android.graphics.Color.parseColor(colorHex)
    val colorRadius = whiteRadius * 0.92f
    drawTeardropPath(canvas, centerX, centerY, colorRadius, tipY, paint)
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(centerX, centerY, colorRadius * 0.35f, paint)
    return BitmapDescriptorFactory.fromBitmap(resultBitmap)
}

private fun drawTeardropPath(canvas: Canvas, centerX: Float, centerY: Float, radius: Float, tipY: Float, paint: android.graphics.Paint) {
    val path = android.graphics.Path()
    val distance = tipY - centerY
    if (distance <= radius) {
        canvas.drawCircle(centerX, centerY, radius, paint)
        return
    }
    val theta = kotlin.math.asin(radius / distance)
    val thetaDeg = Math.toDegrees(theta.toDouble()).toFloat()
    path.arcTo(centerX - radius, centerY - radius, centerX + radius, centerY + radius, 90f + thetaDeg, 360f - 2 * thetaDeg, false)
    path.lineTo(centerX, tipY)
    path.close()
    canvas.drawPath(path, paint)
}

private fun getIconByStyle(style: String): ImageVector {
    return when (style) {
        "fish" -> Icons.Default.Star
        else -> Icons.Default.Place
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
        位置：${spot.address}
        坐标：${spot.latitude},${spot.longitude}
        钓点类别：${spot.fee}
        主攻鱼种：${spot.fishSpecies}
        作钓备注：${spot.notes}
        --- 复制此段全部文字，打开爆护 App 即可一键导入钓点！ ---
    """.trimIndent()
}

private fun parseShareCode(text: String): FishingSpot? {
    try {
        val name = Regex("名称[：:](.*)").find(text)?.groupValues?.get(1)?.trim() ?: ""
        val address = Regex("位置[：:](.*)").find(text)?.groupValues?.get(1)?.trim() ?: "分享位置"
        val coords = Regex("坐标[：:]([-0-9.]+)\\s*,\\s*([-0-9.]+)").find(text) ?: return null
        val lat = coords.groupValues[1].toDoubleOrNull() ?: return null
        val lng = coords.groupValues[2].toDoubleOrNull() ?: return null
        val category = Regex("钓点类别[：:](.*)").find(text)?.groupValues?.get(1)?.trim() ?: "手竿"
        val species = Regex("主攻鱼种[：:](.*)").find(text)?.groupValues?.get(1)?.trim() ?: ""
        val notes = Regex("作钓备注[：:](.*)").find(text)?.groupValues?.get(1)?.trim() ?: ""
        return FishingSpot(name = name, address = address, latitude = lat, longitude = lng, iconColor = "#00ADB5", iconStyle = "pin", arrivalTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date()), fishSpecies = species, bait = "", fee = category, notes = notes)
    } catch (e: Exception) { return null }
}

private fun launchAMapNavi(context: android.content.Context, lat: Double, lon: Double) {
    try {
        val uri = "androidamap://navi?sourceApplication=BaoHu&lat=$lat&lon=$lon&dev=0&style=2"
        val intent = android.content.Intent("android.intent.action.VIEW", android.net.Uri.parse(uri))
        intent.setPackage("com.autonavi.minimap")
        context.startActivity(intent)
    } catch (e: Exception) {
        showToast("未检测到高德地图，请先安装")
    }
}
