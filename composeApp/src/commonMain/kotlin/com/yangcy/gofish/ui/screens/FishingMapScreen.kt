package com.yangcy.gofish.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.yangcy.gofish.data.model.FishingSpot
import com.yangcy.gofish.ui.viewmodel.FishViewModel
import com.yangcy.gofish.ui.viewmodel.FishingLocation
import com.yangcy.gofish.ui.viewmodel.formatDecimal
import com.yangcy.gofish.util.analytics
import com.yangcy.gofish.util.openMapNavigation
import com.yangcy.gofish.util.showToast
import kotlinx.coroutines.launch

// --- 纯 KMP 跨平台十六进制颜色解析器 ---
fun parseHexColor(hex: String): Color {
    val cleanHex = hex.removePrefix("#")
    val colorLong = try {
        if (cleanHex.length == 6) {
            ("FF" + cleanHex).toLong(16)
        } else if (cleanHex.length == 8) {
            cleanHex.toLong(16)
        } else {
            0xFF00ADB5L
        }
    } catch (e: Exception) {
        0xFF00ADB5L
    }
    return Color(colorLong)
}

// --- 钓点口令解析模型 ---
data class ParsedShareCode(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val fee: String,
    val fishSpecies: String,
    val notes: String
)

// --- 兼容旧版 URI 与新版中文文本的口令解析器 ---
fun parseShareCode(text: String): ParsedShareCode? {
    val cleanText = text.trim()
    if (cleanText.startsWith("GoFishSpot://")) {
        try {
            val content = cleanText.substringAfter("GoFishSpot://")
            val parts = content.split("#")
            val name = parts[0]
            val coords = parts[1].split(",")
            val lat = coords[0].toDouble()
            val lon = coords[1].toDouble()
            val species = parts.getOrNull(2) ?: "未知"
            val bait = parts.getOrNull(3) ?: "未知"
            val fee = parts.getOrNull(4) ?: "免费"
            return ParsedShareCode(
                name = name,
                latitude = lat,
                longitude = lon,
                fee = fee,
                fishSpecies = species,
                notes = if (bait.isNotEmpty() && bait != "未知") "推荐饵料: $bait" else ""
            )
        } catch (e: Exception) {
            return null
        }
    } else if (cleanText.contains("【爆护钓点分享】") || (cleanText.contains("名称") && cleanText.contains("坐标"))) {
        try {
            val nameMatch = Regex("""名称[：:]\s*(.*)""").find(cleanText)
            val coordsMatch = Regex("""坐标[：:]\s*([\d.-]+)\s*,\s*([\d.-]+)""").find(cleanText)
            val categoryMatch = Regex("""钓点类别[：:]\s*(.*)""").find(cleanText)
            val fishMatch = Regex("""主攻鱼种[：:]\s*(.*)""").find(cleanText)
            val notesMatch = Regex("""作钓备注[：:]\s*([\s\S]*?)(?:\n---|\n建议|$)""").find(cleanText)
            
            if (nameMatch != null && coordsMatch != null) {
                val name = nameMatch.groupValues[1].trim()
                val lat = coordsMatch.groupValues[1].toDouble()
                val lon = coordsMatch.groupValues[2].toDouble()
                val fee = categoryMatch?.groupValues?.get(1)?.trim() ?: "免费"
                val species = fishMatch?.groupValues?.get(1)?.trim() ?: "未知"
                val notes = notesMatch?.groupValues?.get(1)?.trim() ?: ""
                return ParsedShareCode(
                    name = name,
                    latitude = lat,
                    longitude = lon,
                    fee = fee,
                    fishSpecies = species,
                    notes = notes
                )
            }
        } catch (e: Exception) {
            return null
        }
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FishingMapScreen(viewModel: FishViewModel) {
    val spots by viewModel.fishingSpots.collectAsState()
    val selectedLocation by viewModel.selectedLocation.collectAsState()
    val isPositioning by viewModel.isPositioning.collectAsState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current

    var selectedSpot by remember { mutableStateOf<FishingSpot?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }
    var isSatellite by remember { mutableStateOf(false) }
    var zoomLevel by remember { mutableStateOf(15f) }
    var selectedCategory by remember { mutableStateOf("全部") }

    // --- 状态控制：长按准备标记点的坐标与弹窗 ---
    var pendingLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var showPendingLocationBottomSheet by remember { mutableStateOf(false) }
    var showAddSpotDialog by remember { mutableStateOf(false) }
    var showSpotLibraryDrawer by remember { mutableStateOf(false) }
    var showShareCodeDialog by remember { mutableStateOf<String?>(null) }
    var showPasteImportDialog by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var editingSpotId by remember { mutableStateOf(0) }
    
    var tempLat by remember { mutableStateOf(0.0) }
    var tempLon by remember { mutableStateOf(0.0) }
    var tempAddress by remember { mutableStateOf("") }

    // --- 智能解析分享口令 ---
    val parsedImportSpot = remember(searchQuery) { parseShareCode(searchQuery) }
    
    // --- 钓点列表过滤 ---
    val filteredSpots = remember(spots, searchQuery, selectedCategory) {
        spots.filter { spot ->
            val matchesSearch = searchQuery.isEmpty() || parsedImportSpot != null ||
                    spot.name.contains(searchQuery, ignoreCase = true) ||
                    spot.address.contains(searchQuery, ignoreCase = true) ||
                    spot.fishSpecies.contains(searchQuery, ignoreCase = true)

            val matchesCategory = when (selectedCategory) {
                "全部" -> true
                "免费" -> spot.fee == "免费" || spot.fee.contains("免")
                "路亚" -> spot.fishSpecies.contains("路亚") || spot.notes.contains("路亚") || spot.name.contains("路亚") || spot.fee.contains("路亚")
                "野钓" -> spot.fee == "免费" || spot.notes.contains("野") || spot.name.contains("野") || spot.fee.contains("野钓")
                "黑坑" -> spot.fee.contains("收费") || spot.fee.contains("天") || spot.notes.contains("黑坑") || spot.name.contains("黑坑") || spot.fee.contains("黑坑")
                "海竿" -> spot.fee.contains("海") || spot.fishSpecies.contains("海") || spot.notes.contains("海")
                else -> true
            }
            matchesSearch && matchesCategory
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // --- 地图底层 ---
        MapComponent(
            modifier = Modifier.fillMaxSize(),
            spots = filteredSpots,
            selectedLocation = selectedLocation,
            zoomLevel = zoomLevel,
            isSatellite = isSatellite,
            pendingLocation = pendingLocation,
            onMapClick = {
                focusManager.clearFocus()
                isSearchFocused = false
            },
            onMapLongClick = { lat, lon ->
                analytics.trackEvent("map_long_press")
                tempLat = lat
                tempLon = lon
                pendingLocation = Pair(lat, lon)
                isEditing = false
                
                // 地图随长按标记点自动调整中心 (对齐截图 1 效果)
                viewModel.selectLocation(
                    FishingLocation(
                        name = "准备标记点",
                        province = "",
                        lat = lat,
                        lon = lon,
                        prominentSpecies = emptyList(),
                        description = ""
                    )
                )
                showPendingLocationBottomSheet = true
            },
            onMarkerClick = { spot ->
                selectedSpot = spot
                focusManager.clearFocus()
            }
        )

        // --- 顶栏：搜索与分类过滤 ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 2.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 搜索框
            val searchBarWidth by animateFloatAsState(targetValue = if (isSearchFocused) 0.95f else 0.9f)
            Row(
                modifier = Modifier
                    .fillMaxWidth(searchBarWidth)
                    .height(54.dp)
                    .shadow(8.dp, RoundedCornerShape(27.dp))
                    .background(Color.White, RoundedCornerShape(27.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showSpotLibraryDrawer = true }) {
                    Icon(
                        imageVector = Icons.Default.FormatListBulleted,
                        contentDescription = "钓点库",
                        tint = Color(0xFF0066A1)
                    )
                }
                
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("搜索专属钓点或输入口令...", fontSize = 14.sp, color = Color.Gray) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { isSearchFocused = it.isFocused },
                    singleLine = true
                )

                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, "清除", tint = Color.Gray)
                    }
                }
                
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "搜索",
                    tint = Color(0xFF0066A1)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 分类芯片过滤列表
            val categories = listOf("全部", "手竿", "路亚", "海竿", "黑坑", "野钓")
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory == category
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) Color(0xFF0066A1) else Color.White)
                            .border(1.dp, if (isSelected) Color.Transparent else Color(0xFFE0E0E0), RoundedCornerShape(16.dp))
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = category,
                            color = if (isSelected) Color.White else Color.DarkGray,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // --- 控制组 (悬浮按钮) ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = if (selectedSpot != null || showPendingLocationBottomSheet) 320.dp else 40.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 钓点库 i 键
            FloatingActionButton(
                onClick = { showSpotLibraryDrawer = true },
                containerColor = Color.White,
                contentColor = Color(0xFF0066A1),
                shape = CircleShape,
                modifier = Modifier.size(46.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = "钓点信息", modifier = Modifier.size(22.dp))
            }

            // 卫星/普通图层切换
            FloatingActionButton(
                onClick = { isSatellite = !isSatellite },
                containerColor = Color.White,
                contentColor = if (isSatellite) Color(0xFF0066A1) else Color.DarkGray,
                shape = CircleShape,
                modifier = Modifier.size(46.dp)
            ) {
                Icon(
                    imageVector = if (isSatellite) Icons.Default.Layers else Icons.Default.LayersClear,
                    contentDescription = "卫星模式",
                    modifier = Modifier.size(22.dp)
                )
            }

            // 放大按键
            FloatingActionButton(
                onClick = { if (zoomLevel < 20f) zoomLevel += 1f },
                containerColor = Color.White,
                contentColor = Color.DarkGray,
                shape = CircleShape,
                modifier = Modifier.size(46.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "放大", modifier = Modifier.size(22.dp))
            }

            // 缩小按键
            FloatingActionButton(
                onClick = { if (zoomLevel > 3f) zoomLevel -= 1f },
                containerColor = Color.White,
                contentColor = Color.DarkGray,
                shape = CircleShape,
                modifier = Modifier.size(46.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "缩小", modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 定位按键 (大蓝圆)
            FloatingActionButton(
                onClick = { viewModel.triggerPreciseLocation() },
                containerColor = Color(0xFF0066A1),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                if (isPositioning) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Icon(Icons.Default.LocationOn, contentDescription = "定位", modifier = Modifier.size(26.dp))
                }
            }
        }

        // =========================================================================
        // --- 100% 还原截图 1：长按选定钓点经纬度坐标 底部半弹窗 (选定坐标+按钮) ---
        // =========================================================================
        AnimatedVisibility(
            visible = showPendingLocationBottomSheet,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEBEFF5)) // 对齐截图1淡灰紫底色
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "选定钓点经纬度坐标",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "经度: ${formatDecimal(tempLon)}°E",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0066A1)
                        )
                        Spacer(modifier = Modifier.width(18.dp))
                        Text(
                            text = "纬度: ${formatDecimal(tempLat)}°N",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0066A1)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 取消选择按钮
                        OutlinedButton(
                            onClick = {
                                showPendingLocationBottomSheet = false
                                pendingLocation = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(23.dp),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
                            border = BorderStroke(1.dp, Color(0xFFB0BEC5))
                        ) {
                            Text("取消选择", fontSize = 14.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
                        }

                        // 标记并记录钓点按钮
                        Button(
                            onClick = {
                                showPendingLocationBottomSheet = false
                                scope.launch {
                                    tempAddress = viewModel.getAddressFromLocation(tempLat, tempLon)
                                    showAddSpotDialog = true
                                }
                            },
                            modifier = Modifier
                                .weight(1.4f)
                                .height(46.dp),
                            shape = RoundedCornerShape(23.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066A1))
                        ) {
                            Text("+ 标记并记录钓点", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- 底部选中钓点详情卡片 ---
        AnimatedVisibility(
            visible = selectedSpot != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            selectedSpot?.let { spot ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        // 顶部灰色拖动条指示器
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray)
                                .align(Alignment.CenterHorizontally)
                        )
                        
                        Spacer(modifier = Modifier.height(14.dp))

                        // 第一行：特制圆角青色 Logo 及标题信息
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFE0F7FA)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFF00ADB5),
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = spot.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A1A1B)
                                )
                                Text(
                                    text = "${spot.fee}钓点",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            IconButton(
                                onClick = { selectedSpot = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color.Gray)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 第二行：分类胶囊药丸 (Pills)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 推荐饵料/技法标签
                            val baitPillText = spot.bait.substringBefore("、").substringBefore("，").ifEmpty { "手竿" }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE8F5E9))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = baitPillText,
                                    color = Color(0xFF2E7D32),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // 鱼种标签
                            val fishPillText = spot.fishSpecies.substringBefore("、").substringBefore("，").ifEmpty { "未知" }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE3F2FD))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🐟", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = fishPillText,
                                    color = Color(0xFF1565C0),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 第三行：实战技巧/备注卡片
                        val rawNotes = spot.notes
                        val notesDisplay = rawNotes.substringBefore("\n[Rating:")
                        val spotRating = rawNotes.substringAfter("\n[Rating:", "5.0").substringBefore("]").toFloatOrNull() ?: 5.0f

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("⭐", fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "实战技巧/备注",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1A1A1B)
                                        )
                                    }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "★".repeat(spotRating.toInt()) + if (spotRating % 1f >= 0.5f) "☆" else "",
                                            color = Color(0xFFFFB300),
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = " ${spotRating}分",
                                            color = Color.Gray,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = notesDisplay.ifEmpty { "暂无备注" },
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 第四行：操作按钮组 (高德导航 + 编辑)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    analytics.trackEvent("launch_navi")
                                    openMapNavigation(spot.latitude, spot.longitude, spot.name)
                                },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066A1))
                            ) {
                                Text("▶ 开启导航", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    tempLat = spot.latitude
                                    tempLon = spot.longitude
                                    tempAddress = spot.address
                                    isEditing = true
                                    editingSpotId = spot.id
                                    showAddSpotDialog = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray),
                                border = BorderStroke(1.dp, Color(0xFFCCCCCC))
                            ) {
                                Text("编辑", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // --- 100% 还原截图 2：“标记新钓点” 表单对话框 (预设芯片+颜色选择器+地址) ---
        // =========================================================================
        if (showAddSpotDialog) {
            val initialNotes = if (isEditing) selectedSpot?.notes.orEmpty() else ""
            val initialNotesDisplay = initialNotes.substringBefore("\n[Rating:")

            val namePresets = listOf("我的钓点", "河道水域", "秘密基地", "野钓自留地")
            val typePresets = listOf("手竿", "路亚", "海竿", "黑坑", "野钓")
            val colorsList = listOf("#00ADB5", "#FF5252", "#4CAF50", "#FFB300", "#9C27B0")

            var spotName by remember { mutableStateOf(if (isEditing) selectedSpot?.name.orEmpty() else "我的钓点") }
            var selectedType by remember { mutableStateOf(if (isEditing) selectedSpot?.fee.orEmpty() else "手竿") }
            var selectedColorHex by remember { mutableStateOf(if (isEditing) selectedSpot?.iconColor.orEmpty() else "#00ADB5") }
            var fishType by remember { mutableStateOf(if (isEditing) selectedSpot?.fishSpecies.orEmpty() else "") }
            var baitType by remember { mutableStateOf(if (isEditing) selectedSpot?.bait.orEmpty() else "面饵") }
            var notesText by remember { mutableStateOf(initialNotesDisplay) }
            
            Dialog(
                onDismissRequest = { 
                    showAddSpotDialog = false 
                    isEditing = false
                }
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .wrapContentHeight()
                        .shadow(20.dp, RoundedCornerShape(28.dp)),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECEFF4)) // 淡灰紫卡片
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = if (isEditing) "修改专属钓点" else "标记新钓点",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1B)
                        )

                        // 1. 选择钓点名称:
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("选择钓点名称:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0066A1))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(namePresets) { preset ->
                                    val isSelected = spotName == preset
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) Color(0xFFD0E6FF) else Color.White)
                                            .border(1.dp, if (isSelected) Color(0xFF0066A1) else Color(0xFFE0E0E0), RoundedCornerShape(10.dp))
                                            .clickable { spotName = preset }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = preset,
                                            color = if (isSelected) Color(0xFF0066A1) else Color.DarkGray,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        // 2. 选择钓点类别:
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("选择钓点类别:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0066A1))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(typePresets) { type ->
                                    val isSelected = selectedType == type
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) Color(0xFFD0E6FF) else Color.White)
                                            .border(1.dp, if (isSelected) Color(0xFF0066A1) else Color(0xFFE0E0E0), RoundedCornerShape(10.dp))
                                            .clickable { selectedType = type }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = type,
                                            color = if (isSelected) Color(0xFF0066A1) else Color.DarkGray,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        // 3. 详细地址 (已自动获取)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("详细地址 (已自动获取)", fontSize = 13.sp, color = Color.Gray)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFFCFD8DC), RoundedCornerShape(10.dp))
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = tempAddress.ifEmpty { "定位获取中..." },
                                    fontSize = 14.sp,
                                    color = Color(0xFF2C3E50),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // 4. 标记颜色:
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("标记颜色:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                colorsList.forEach { hex ->
                                    val isSelected = selectedColorHex == hex
                                    val circleColor = parseHexColor(hex)
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(circleColor)
                                            .border(
                                                width = if (isSelected) 3.dp else 0.dp,
                                                color = if (isSelected) Color.Black else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { selectedColorHex = hex }
                                    )
                                }
                            }
                        }

                        // 5. 目标鱼种 (必填)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = fishType,
                                onValueChange = { fishType = it },
                                placeholder = { Text("目标鱼种 (必填)", fontSize = 14.sp, color = Color.Gray) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White, RoundedCornerShape(10.dp)),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF0066A1),
                                    unfocusedBorderColor = Color(0xFFCFD8DC)
                                )
                            )
                        }

                        // 底部操作区 (取消 + 保存)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { 
                                    showAddSpotDialog = false 
                                    isEditing = false
                                    pendingLocation = null
                                }
                            ) {
                                Text("取消", color = Color(0xFF0066A1), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Button(
                                onClick = {
                                    val newSpot = FishingSpot(
                                        id = if (isEditing) editingSpotId else 0,
                                        name = spotName.ifEmpty { "我的钓点" },
                                        address = tempAddress,
                                        latitude = tempLat,
                                        longitude = tempLon,
                                        iconColor = selectedColorHex,
                                        iconStyle = when (selectedType) {
                                            "路亚" -> "fish"
                                            "黑坑" -> "anchor"
                                            "野钓" -> "flag"
                                            "手竿" -> "star"
                                            else -> "default"
                                        },
                                        arrivalTime = "未知",
                                        imageUrl = null,
                                        fishSpecies = fishType.ifEmpty { "通用鱼种" },
                                        bait = baitType,
                                        fee = selectedType,
                                        notes = notesText,
                                        isSynced = false
                                    )
                                    viewModel.addFishingSpot(newSpot)
                                    analytics.trackEvent(if (isEditing) "edit_fishing_spot" else "add_fishing_spot")
                                    
                                    if (isEditing) {
                                        selectedSpot = newSpot
                                    }
                                    
                                    showAddSpotDialog = false
                                    isEditing = false
                                    pendingLocation = null
                                    showToast(if (isEditing) "修改已保存" else "钓点已存入密库")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066A1)),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                                modifier = Modifier.height(42.dp)
                            ) {
                                Text("保存", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }

        // --- 专属钓点库左侧滑出抽屉 ---
        AnimatedVisibility(
            visible = showSpotLibraryDrawer,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { showSpotLibraryDrawer = false }
            )
        }

        AnimatedVisibility(
            visible = showSpotLibraryDrawer,
            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.85f)
                .align(Alignment.CenterStart)
        ) {
            Card(
                shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxHeight()
                    .shadow(16.dp, RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatListBulleted,
                                contentDescription = "列表",
                                tint = Color(0xFF0066A1),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "我的钓点库 (${spots.size})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1A1B)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE3F2FD))
                                .clickable { showPasteImportDialog = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = "导入",
                                tint = Color(0xFF0066A1),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "导入口令",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0066A1)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { showSpotLibraryDrawer = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = Color.Gray
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        if (spots.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOff,
                                            contentDescription = null,
                                            tint = Color.LightGray,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "暂无专属钓点，长按地图即可添加标记",
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            items(spots) { spot ->
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7FA)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.selectLocation(
                                                FishingLocation(
                                                    name = spot.name,
                                                    province = "本地水域",
                                                    lat = spot.latitude,
                                                    lon = spot.longitude,
                                                    prominentSpecies = spot.fishSpecies.split("、"),
                                                    description = spot.notes
                                                )
                                            )
                                            selectedSpot = spot
                                            showSpotLibraryDrawer = false
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF00ADB5))
                                        )

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = spot.name,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1A1A1B),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                val badgeText = spot.fishSpecies.substringBefore("、").substringBefore("，").take(4)
                                                if (badgeText.isNotEmpty()) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(Color(0xFFE3F2FD))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = badgeText,
                                                            color = Color(0xFF0066A1),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            val latStr = spot.latitude.toString().let { if (it.length > 8) it.take(8) else it }
                                            val lonStr = spot.longitude.toString().let { if (it.length > 9) it.take(9) else it }
                                            Text(
                                                text = if (spot.address != "口令导入点") spot.address else "好友分享 • 纬度:${latStr}, 经度:${lonStr}",
                                                fontSize = 11.sp,
                                                color = Color.Gray,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    val code = """
                                                    【爆护钓点分享】
                                                    名称：${spot.name}
                                                    位置：${spot.address}
                                                    坐标：${spot.latitude},${spot.longitude}
                                                    钓点类别：${spot.fee}
                                                    主攻鱼种：${spot.fishSpecies}
                                                    作钓备注：${spot.notes.substringBefore("\n[Rating:").ifEmpty { "无" }}
                                                    --- 复制此段全部文字，打开 GoFish 即可一键导入钓点！ ---
                                                    """.trimIndent()
                                                    showShareCodeDialog = code
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Share,
                                                    contentDescription = "分享",
                                                    tint = Color(0xFF0066A1),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    viewModel.deleteFishingSpot(spot)
                                                    if (selectedSpot?.id == spot.id) {
                                                        selectedSpot = null
                                                    }
                                                    showToast("已删除该专属钓点")
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "删除",
                                                    tint = Color.Red,
                                                    modifier = Modifier.size(18.dp)
                                                )
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

        // --- 手动粘入口令输入弹窗 ---
        if (showPasteImportDialog) {
            var pasteText by remember { mutableStateOf("") }
            Dialog(
                onDismissRequest = { showPasteImportDialog = false }
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .wrapContentHeight()
                        .shadow(16.dp, RoundedCornerShape(28.dp)),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "导入分享钓点口令",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1B)
                        )
                        
                        Text(
                            text = "粘贴好友发送的『爆护口令』全文文本，即可一键同步该钓点。",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            lineHeight = 18.sp
                        )
                        
                        OutlinedTextField(
                            value = pasteText,
                            onValueChange = { pasteText = it },
                            placeholder = { Text("在此粘贴口令...", fontSize = 13.sp, color = Color.LightGray) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0066A1),
                                unfocusedBorderColor = Color(0xFFE0E0E0),
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { showPasteImportDialog = false }
                            ) {
                                Text("取消", color = Color(0xFF0066A1), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Button(
                                onClick = {
                                    val parsed = parseShareCode(pasteText)
                                    if (parsed != null) {
                                        searchQuery = pasteText.trim()
                                        showPasteImportDialog = false
                                    } else {
                                        showToast("未匹配到口令格式")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066A1)),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Text("确认解析", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        // --- 自动口令导入弹窗 ---
        if (parsedImportSpot != null) {
            Dialog(
                onDismissRequest = { searchQuery = "" }
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .wrapContentHeight()
                        .shadow(16.dp, RoundedCornerShape(28.dp)),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "💬 导入分享钓点",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1B)
                        )
                        
                        Text(
                            text = "确定要在地图上导入并创建该好友分享的钓点吗？",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF5F7FA), RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row {
                                Text("名称：", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.DarkGray)
                                Text(parsedImportSpot.name, fontSize = 13.sp, color = Color.Black)
                            }
                            Row {
                                Text("类别：", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.DarkGray)
                                Text(parsedImportSpot.fee, fontSize = 13.sp, color = Color.Black)
                            }
                            Row {
                                Text("鱼种：", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.DarkGray)
                                Text(parsedImportSpot.fishSpecies, fontSize = 13.sp, color = Color.Black)
                            }
                            if (parsedImportSpot.notes.isNotEmpty()) {
                                Row {
                                    Text("备注：", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.DarkGray)
                                    Text(parsedImportSpot.notes, fontSize = 13.sp, color = Color.Black, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { searchQuery = "" }
                            ) {
                                Text("忽略", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Button(
                                onClick = {
                                    viewModel.addFishingSpot(
                                        FishingSpot(
                                            name = parsedImportSpot.name,
                                            address = "口令导入点",
                                            latitude = parsedImportSpot.latitude,
                                            longitude = parsedImportSpot.longitude,
                                            iconColor = "#FF00ADB5",
                                            iconStyle = "default",
                                            arrivalTime = "未知",
                                            fishSpecies = parsedImportSpot.fishSpecies,
                                            bait = "通用饵料",
                                            fee = parsedImportSpot.fee,
                                            notes = parsedImportSpot.notes
                                        )
                                    )
                                    searchQuery = ""
                                    analytics.trackEvent("import_spot_code")
                                    showToast("钓点导入成功！")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066A1)),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Text("确认导入", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        // --- 分享口令复制确认弹窗 ---
        showShareCodeDialog?.let { code ->
            AlertDialog(
                onDismissRequest = { showShareCodeDialog = null },
                title = { Text("生成分享口令", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("复制以下口令发送给好友，好友在搜索框粘贴或打开菜单导入即可：", fontSize = 13.sp, color = Color.DarkGray)
                        OutlinedTextField(
                            value = code,
                            onValueChange = {},
                            readOnly = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(code))
                            showToast("分享口令已复制到剪贴板")
                            showShareCodeDialog = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066A1))
                    ) { Text("复制口令", color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { showShareCodeDialog = null }) { Text("关闭") }
                }
            )
        }
    }
}
