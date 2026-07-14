package com.yangcy.gofish.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
import gofish.composeapp.generated.resources.Res
import gofish.composeapp.generated.resources.ic_launcher_foreground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun FishingMapScreen(viewModel: FishViewModel) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

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
    
    // AMap Instance using TextureMapView
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

    // 剪贴板识别 (Clipboard Recognition)
    val windowInfo = androidx.compose.ui.platform.LocalWindowInfo.current
    LaunchedEffect(windowInfo.isWindowFocused) {
        if (windowInfo.isWindowFocused) {
            try {
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                if (clipboard.hasPrimaryClip()) {
                    val clipData = clipboard.primaryClip
                    if (clipData != null && clipData.itemCount > 0) {
                        val text = clipData.getItemAt(0).text?.toString() ?: ""
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
            } catch (e: Exception) {}
        }
    }

    // Lifecycle
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
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Initial Camera
    var initialCameraSet by remember { mutableStateOf(false) }
    LaunchedEffect(aMap) {
        val map = aMap ?: return@LaunchedEffect
        if (!initialCameraSet) {
            val lastLat = sharedPrefs.getFloat("last_lat", selectedLocation.lat.toFloat()).toDouble()
            val lastLon = sharedPrefs.getFloat("last_lon", selectedLocation.lon.toFloat()).toDouble()
            val lastZoom = sharedPrefs.getFloat("last_zoom", 15f)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lastLat, lastLon), lastZoom))
            initialCameraSet = true
        }
    }

    // Filtered Spots
    val filteredSpots = remember(spots, activeCategory) {
        if (activeCategory == "全部") spots
        else spots.filter { it.fee == activeCategory }
    }

    // Marker Updates
    LaunchedEffect(filteredSpots, aMap, selectedTempCoords) {
        val map = aMap ?: return@LaunchedEffect
        map.clear() 
        markerMap.clear()
        
        val myLocationStyle = MyLocationStyle()
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
        myLocationStyle.showMyLocation(true)
        myLocationStyle.radiusFillColor(android.graphics.Color.argb(20, 30, 136, 229))
        myLocationStyle.strokeColor(android.graphics.Color.argb(40, 30, 136, 229))
        myLocationStyle.strokeWidth(1f)
        
        myLocationStyle.myLocationIcon(getResizedBitmapDescriptor(context, com.yangcy.gofish.R.drawable.ic_launcher_foreground, 140, 140, isMyLocation = true))
        map.myLocationStyle = myLocationStyle
        map.isMyLocationEnabled = true

        filteredSpots.forEach { spot ->
            val pos = LatLng(spot.latitude, spot.longitude)
            val markerOptions = MarkerOptions().position(pos).title(spot.name).snippet(spot.address)
                .icon(getResizedDefaultMarker(context, 80, 100, spot.iconColor))
            val marker = map.addMarker(markerOptions)
            marker.`object` = spot
            markerMap[spot.id] = marker
        }

        selectedTempCoords?.let { coords ->
            val markerOptions = MarkerOptions().position(coords).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
            map.addMarker(markerOptions)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                if (aMap == null) {
                    val map = view.map
                    map.uiSettings.apply {
                        isZoomControlsEnabled = false
                        isCompassEnabled = true
                        isScaleControlsEnabled = true
                        isMyLocationButtonEnabled = false
                    }
                    map.setOnCameraChangeListener(object : AMap.OnCameraChangeListener {
                        override fun onCameraChange(p0: com.amap.api.maps.model.CameraPosition?) {}
                        override fun onCameraChangeFinish(pos: com.amap.api.maps.model.CameraPosition?) {
                            pos?.let {
                                sharedPrefs.edit()
                                    .putFloat("last_lat", it.target.latitude.toFloat())
                                    .putFloat("last_lon", it.target.longitude.toFloat())
                                    .putFloat("last_zoom", it.zoom).apply()
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
                            true
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
                .align(Alignment.TopStart),
            horizontalAlignment = Alignment.Start
        ) {
            val searchBarWidth by animateFloatAsState(targetValue = if (isSearchFocused) 1f else 0.5f)
            
            Row(
                modifier = Modifier
                    .fillMaxWidth(searchBarWidth)
                    .height(56.dp)
                    .padding(horizontal = 16.dp)
                    .shadow(8.dp, RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), RoundedCornerShape(28.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(28.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { if (isClickAllowed()) showSpotList = !showSpotList }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.List, contentDescription = null, tint = if (showSpotList) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(4.dp))
                TextField(
                    value = searchQuery,
                    onValueChange = { query ->
                        searchQuery = query
                        val matchedSpot = spots.firstOrNull { it.name.contains(query, ignoreCase = true) }
                        if (matchedSpot != null) {
                            aMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(matchedSpot.latitude, matchedSpot.longitude), 15f))
                        }
                    },
                    placeholder = { Text("搜索我的钓点...", fontSize = 14.sp) },
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, disabledContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                    singleLine = true,
                    modifier = Modifier.weight(1f).onFocusChanged { isSearchFocused = it.isFocused }
                )
                if (isSearchFocused || searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = ""; isSearchFocused = false; focusManager.clearFocus() }) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(20.dp))
                    }
                }
                IconButton(onClick = {
                    if (isClickAllowed()) {
                        if (searchQuery.trim().isNotEmpty()) {
                            val matchedSpot = spots.firstOrNull { it.name.contains(searchQuery, ignoreCase = true) }
                            if (matchedSpot != null) {
                                focusManager.clearFocus(); isSearchFocused = false
                                showToast("已为您定位至『${matchedSpot.name}』")
                                aMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(matchedSpot.latitude, matchedSpot.longitude), 16f))
                                searchQuery = ""
                            } else { showToast("未找到名为『$searchQuery』的钓点") }
                        } else { isSearchFocused = true }
                    }
                }) { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }

            AnimatedVisibility(visible = !isSearchFocused, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                LazyRow(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val categories = listOf("全部", "手竿", "路亚", "海竿", "黑坑", "野钓", "水库")
                    items(categories) { cat ->
                        val isSelected = activeCategory == cat
                        FilterChip(selected = isSelected, onClick = { activeCategory = cat; focusManager.clearFocus() }, label = { Text(cat, fontSize = 12.sp) }, shape = RoundedCornerShape(16.dp))
                    }
                }
            }
        }

        // ------------------ ON-MAP OVERLAYS ------------------
        Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 120.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MapControlButton(Icons.Default.Info) { if (isClickAllowed()) viewDocType = DocType.PRIVACY_POLICY }
            MapControlButton(Icons.Default.Settings) { if (isClickAllowed()) {
                selectedMapType = if (selectedMapType == AMap.MAP_TYPE_NORMAL) AMap.MAP_TYPE_SATELLITE else AMap.MAP_TYPE_NORMAL
                aMap?.mapType = selectedMapType
            }}
            Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { aMap?.animateCamera(CameraUpdateFactory.zoomIn()) }) { Icon(Icons.Default.Add, null) }
                    HorizontalDivider(modifier = Modifier.width(20.dp), thickness = 0.5.dp)
                    IconButton(onClick = { aMap?.animateCamera(CameraUpdateFactory.zoomOut()) }) { Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) { Box(modifier = Modifier.size(12.dp, 2.dp).background(MaterialTheme.colorScheme.onSurface)) } }
                }
            }
            FloatingActionButton(
                onClick = { if (isClickAllowed() && !isPositioning) {
                    viewModel.triggerPreciseLocation(onFrequentRequest = { showToast("请求过于频繁") }, onResult = { name -> showToast("定位成功: $name") })
                    aMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(selectedLocation.lat, selectedLocation.lon), 15f))
                }},
                containerColor = if (isPositioning) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                contentColor = if (isPositioning) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                shape = CircleShape, modifier = Modifier.size(54.dp)
            ) {
                if (isPositioning) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(26.dp))
            }
        }

        // ------------------ BOTTOM PANEL ------------------
        selectedTempCoords?.let { coords ->
            Card(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp).shadow(12.dp, RoundedCornerShape(20.dp)), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("选定钓点经纬度坐标", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("经度: ${String.format("%.4f", coords.longitude)}°E   纬度: ${String.format("%.4f", coords.latitude)}°N", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { selectedTempCoords = null }, modifier = Modifier.weight(1f)) { Text("取消选择") }
                        Button(onClick = { showAddSpotDialog = true }, modifier = Modifier.weight(1.5f)) { Text("标记并记录钓点") }
                    }
                }
            }
        }

        if (selectedSpot != null) {
            ModalBottomSheet(onDismissRequest = { selectedSpot = null }, containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
                val spot = selectedSpot!!
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp).padding(bottom = 32.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(Color(android.graphics.Color.parseColor(spot.iconColor)).copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                            Icon(imageVector = getIconByStyle(spot.iconStyle), contentDescription = null, tint = Color(android.graphics.Color.parseColor(spot.iconColor)), modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column { Text(spot.name, fontSize = 20.sp, fontWeight = FontWeight.Black); Text(spot.address, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DetailInfoChip(Icons.Default.Star, spot.fee, Color(0xFFE8F5E9), Color(0xFF2E7D32))
                        DetailInfoChip(Icons.Default.Face, spot.fishSpecies, Color(0xFFE3F2FD), Color(0xFF1565C0))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(spot.notes.ifEmpty { "暂无备注" }, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { launchAMapNavi(context, spot.latitude, spot.longitude) }, modifier = Modifier.weight(1.5f).height(48.dp)) { Text("高德导航") }
                        OutlinedButton(onClick = { showEditSpotDialog = true }, modifier = Modifier.weight(1f).height(48.dp)) { Text("编辑") }
                    }
                }
            }
        }

        // Drawer
        AnimatedVisibility(visible = showSpotList, enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(), exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(), modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(top = 90.dp, start = 16.dp, end = 16.dp, bottom = 100.dp).widthIn(max = 360.dp).fillMaxHeight()) {
            Card(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.List, null, tint = MaterialTheme.colorScheme.primary); Spacer(modifier = Modifier.width(8.dp))
                        Text(if(activeCategory == "全部") "我的钓点库 (${spots.size})" else "$activeCategory 钓点 (${filteredSpots.size})", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        IconButton(onClick = { showSpotList = false }) { Icon(Icons.Default.Close, null) }
                    }
                    androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                        items(filteredSpots) { spot ->
                            Card(modifier = Modifier.fillMaxWidth().clickable { selectedSpot = spot; showSpotList = false; aMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(spot.latitude, spot.longitude), 15f)) }) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(spot.iconColor)))); Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) { Text(spot.name, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text(spot.address, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                    IconButton(onClick = { 
                                        val code = generateShareCode(spot)
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(code))
                                        lastProcessedClipText = code
                                        sharedPrefs.edit().putString("last_processed_clip", code).apply()
                                        showToast("口令已复制到剪贴板！")
                                    }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Share, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) }
                                    IconButton(onClick = { spotToDelete = spot }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)) }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (viewDocType != null) { DocDetailView(type = viewDocType!!, onBack = { viewDocType = null }) }
    }

    // Dialogs
    if (showAddSpotDialog && selectedTempCoords != null) {
        AddSpotDialog(context, selectedTempCoords!!, onDismiss = { showAddSpotDialog = false }, onSave = { viewModel.addFishingSpot(it) })
    }
    if (showEditSpotDialog && selectedSpot != null) {
        EditSpotDialog(selectedSpot!!, onDismiss = { showEditSpotDialog = false }, onSave = { viewModel.addFishingSpot(it) })
    }
    if (showImportDialog) {
        ImportDialog(onDismiss = { showImportDialog = false }, onImport = { spot, rawText -> 
            viewModel.addFishingSpot(spot)
            lastProcessedClipText = rawText
            sharedPrefs.edit().putString("last_processed_clip", rawText).apply()
            showToast("导入成功")
        })
    }
    if (detectedImportSpot != null) {
        AlertDialog(onDismissRequest = { detectedImportSpot = null }, title = { Text("识别到分享钓点") }, text = { Text("是否导入好友分享的钓点『${detectedImportSpot!!.name}』？") }, confirmButton = { Button(onClick = { viewModel.addFishingSpot(detectedImportSpot!!); aMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(detectedImportSpot!!.latitude, detectedImportSpot!!.longitude), 15f)); detectedImportSpot = null }) { Text("确认导入") } }, dismissButton = { TextButton(onClick = { detectedImportSpot = null }) { Text("忽略") } })
    }
}

@Composable
fun MapControlButton(icon: ImageVector, onClick: () -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.size(44.dp).clickable { onClick() }) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) }
    }
}

// Dialog Components
@Composable
fun AddSpotDialog(context: android.content.Context, coords: LatLng, onDismiss: () -> Unit, onSave: (FishingSpot) -> Unit) {
    var name by remember { mutableStateOf("") }
    var species by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("标记新钓点") }, text = { Column { OutlinedTextField(name, {name=it}, label = {Text("名称")}); OutlinedTextField(species, {species=it}, label = {Text("鱼种")}) } }, confirmButton = { Button(onClick = { onSave(FishingSpot(name=name, address="坐标位置", latitude=coords.latitude, longitude=coords.longitude, iconColor="#00ADB5", iconStyle="pin", arrivalTime="", fishSpecies=species, bait="", fee="手竿", notes="")); onDismiss() }) { Text("保存") } })
}

@Composable
fun EditSpotDialog(spot: FishingSpot, onDismiss: () -> Unit, onSave: (FishingSpot) -> Unit) {
    var name by remember { mutableStateOf(spot.name) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("编辑钓点") }, text = { OutlinedTextField(name, {name=it}, label = {Text("名称")}) }, confirmButton = { Button(onClick = { onSave(spot.copy(name=name)); onDismiss() }) { Text("保存") } })
}

@Composable
fun ImportDialog(onDismiss: () -> Unit, onImport: (FishingSpot, String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("导入口令") }, text = { OutlinedTextField(text, {text=it}, label = {Text("粘贴口令")}) }, confirmButton = { Button(onClick = { val s = parseShareCode(text); if(s!=null) onImport(s, text); onDismiss() }) { Text("导入") } })
}

// Helpers
private fun getResizedBitmapDescriptor(context: android.content.Context, resourceId: Int, width: Int, height: Int, isMyLocation: Boolean): com.amap.api.maps.model.BitmapDescriptor {
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
        paint.color = themeColor
        canvas.drawCircle(centerX, centerY, colorRadius * 0.6f, paint)
        drawable?.let {
            it.setTint(android.graphics.Color.WHITE)
            val iconSize = (colorRadius * 0.8f).toInt()
            it.setBounds((centerX - iconSize/2).toInt(), (centerY - iconSize/2).toInt(), (centerX + iconSize/2).toInt(), (centerY + iconSize/2).toInt())
            it.draw(canvas)
        }
    }
    return BitmapDescriptorFactory.fromBitmap(resultBitmap)
}

private fun getResizedDefaultMarker(context: android.content.Context, width: Int, height: Int, colorHex: String): com.amap.api.maps.model.BitmapDescriptor {
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
    if (distance <= radius) { canvas.drawCircle(centerX, centerY, radius, paint); return }
    val theta = kotlin.math.asin(radius / distance)
    val thetaDeg = Math.toDegrees(theta.toDouble()).toFloat()
    path.arcTo(centerX - radius, centerY - radius, centerX + radius, centerY + radius, 90f + thetaDeg, 360f - 2 * thetaDeg, false)
    path.lineTo(centerX, tipY); path.close()
    canvas.drawPath(path, paint)
}

private fun getIconByStyle(style: String): ImageVector = when (style) { "fish" -> Icons.Default.Star; else -> Icons.Default.Place }

@Composable
private fun DetailInfoChip(icon: ImageVector, label: String, containerColor: Color, contentColor: Color) {
    Surface(color = containerColor, shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = contentColor, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(6.dp)); Text(label, color = contentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
