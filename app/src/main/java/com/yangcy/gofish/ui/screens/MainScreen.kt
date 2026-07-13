package com.yangcy.gofish.ui.screens

import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbCloudy
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yangcy.gofish.data.model.Fish
import com.yangcy.gofish.ui.viewmodel.FishViewModel
import com.yangcy.gofish.util.AnalyticsManager

private fun getWeatherIcon(condition: String): ImageVector {
    return when {
        condition.contains("晴") -> Icons.Outlined.WbSunny
        condition.contains("雷") -> Icons.Outlined.Thunderstorm
        condition.contains("雨") -> Icons.Outlined.WaterDrop
        condition.contains("云") -> Icons.Outlined.WbCloudy
        condition.contains("雾") -> Icons.Outlined.Cloud
        else -> Icons.Outlined.Cloud
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: FishViewModel) {
    var currentTab by remember { mutableStateOf(0) } // 0: 图鉴, 1: 渔获记录
    var selectedFish by remember { mutableStateOf<Fish?>(null) }
    var showAddCatchDialog by remember { mutableStateOf(false) }
    var showSplash by remember { mutableStateOf(true) }

    // Auto-dismiss splash screen after 2.5 seconds
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2500)
        showSplash = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main App Content
        if (selectedFish != null) {
            BackHandler {
                selectedFish = null
            }
            // Track Fish Detail Page
            DisposableEffect(selectedFish) {
                AnalyticsManager.onPageStart("FishDetail_${selectedFish?.name ?: "Unknown"}")
                onDispose {
                    AnalyticsManager.onPageEnd("FishDetail_${selectedFish?.name ?: "Unknown"}")
                }
            }
            FishDetailScreen(
                fish = selectedFish!!,
                viewModel = viewModel,
                onBack = { selectedFish = null }
            )
        } else {
            Scaffold(
                bottomBar = {
                    val outlineColor = MaterialTheme.colorScheme.outline
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .drawBehind {
                                drawLine(
                                    color = outlineColor,
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, 0f),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                    ) {
                        val tabs = listOf(
                            Triple("钓点地图", Icons.Default.LocationOn, Icons.Default.LocationOn),
                            Triple("鱼类图鉴", Icons.Default.Home, Icons.Default.Home),
                            Triple("渔获日记", Icons.Default.List, Icons.Default.List)
                        )
                        tabs.forEachIndexed { index, (label, filledIcon, outlinedIcon) ->
                            val isSelected = currentTab == index
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { currentTab = index },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) filledIcon else outlinedIcon,
                                        contentDescription = label,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // State-preserving Tab content
                    // FishingMapScreen is kept in composition to avoid reloading
                    Box(modifier = Modifier.fillMaxSize().alpha(if (currentTab == 0) 1f else 0f)) {
                        FishingMapScreen(viewModel = viewModel)
                        if (currentTab != 0) {
                            // Transparent overlay to prevent interactions when not active
                            Box(modifier = Modifier.fillMaxSize().clickable(enabled = false) {})
                        }
                    }

                    // Umeng Page Tracking (Tab-based)
                    val context = LocalContext.current
                    DisposableEffect(currentTab) {
                        val pageName = when(currentTab) {
                            0 -> "FishingMap"
                            1 -> "FishCatalog"
                            2 -> "CatchHistory"
                            else -> "Unknown"
                        }
                        AnalyticsManager.onPageStart(pageName)
                        AnalyticsManager.trackEvent(context, AnalyticsManager.EVENT_TAB_SWITCH, mapOf("tab_index" to currentTab))
                        
                        onDispose {
                            AnalyticsManager.onPageEnd(pageName)
                        }
                    }

                    if (currentTab == 1) {
                        FishCatalogTab(
                            viewModel = viewModel,
                            onFishClick = { selectedFish = it }
                        )
                    }

                    if (currentTab == 2) {
                        CatchHistoryTab(
                            viewModel = viewModel,
                            onAddClick = { showAddCatchDialog = true }
                        )
                    }

                    if (showAddCatchDialog) {
                        AddCatchDialog(
                            viewModel = viewModel,
                            onDismiss = { showAddCatchDialog = false }
                        )
                    }
                }
            }
        }

        // Splash Screen Overlay with fade-out animation
        DisposableEffect(showSplash) {
            if (showSplash) {
                AnalyticsManager.onPageStart("Splash")
            }
            onDispose {
                if (showSplash) {
                    AnalyticsManager.onPageEnd("Splash")
                }
            }
        }
        AnimatedVisibility(
            visible = showSplash,
            enter = fadeIn(),
            exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(600))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF071118),
                                Color(0xFF0F2537),
                                Color(0xFF1D3C52)
                            )
                        )
                    )
            ) {
                // Background artistic waves drawn with Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    
                    // Water wave paths
                    val path1 = androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, h * 0.72f)
                        cubicTo(w * 0.25f, h * 0.68f, w * 0.75f, h * 0.76f, w, h * 0.70f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(
                        path = path1,
                        color = Color(0xFF00ADB5).copy(alpha = 0.12f)
                    )
                    
                    val path2 = androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, h * 0.76f)
                        cubicTo(w * 0.3f, h * 0.81f, w * 0.7f, h * 0.72f, w, h * 0.77f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(
                        path = path2,
                        color = Color(0xFF00ADB5).copy(alpha = 0.18f)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .systemBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top tag
                    Text(
                        text = "EXPLORE • RECORD • FORECAST",
                        color = Color(0xFF00ADB5).copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(top = 40.dp)
                    )

                    // Middle main title & logo area
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(40.dp))
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF00ADB5).copy(alpha = 0.25f),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .border(1.dp, Color(0xFF00ADB5).copy(alpha = 0.2f), RoundedCornerShape(40.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            // Using local vector logo instead of remote URL for stability and speed
                            Image(
                                painter = painterResource(id = com.yangcy.gofish.R.drawable.ic_launcher_foreground),
                                contentDescription = "App Logo",
                                modifier = Modifier.size(100.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "今天，你爆护了吗",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "专业级垂钓百科与气象实战助手",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.65f),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Bottom Enter Button & skip action
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 30.dp)
                    ) {
                        val context = LocalContext.current
                        Button(
                            onClick = { 
                                AnalyticsManager.trackEvent(context, AnalyticsManager.EVENT_SPLASH_ENTER)
                                showSplash = false 
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00ADB5),
                                contentColor = Color(0xFF071118)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .padding(horizontal = 24.dp)
                        ) {
                            Text(
                                text = "立即开启爆护之旅",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "自动跳转中...",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FishCatalogTab(
    viewModel: FishViewModel,
    onFishClick: (Fish) -> Unit
) {
    val fishList by viewModel.filteredFishList.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val selectedLocation by viewModel.selectedLocation.collectAsState()
    val isWeatherLoading by viewModel.weatherLoading.collectAsState()
    val weatherTemp by viewModel.weatherTemp.collectAsState()
    val weatherCond by viewModel.weatherCondition.collectAsState()
    val weatherWind by viewModel.weatherWind.collectAsState()

    val context = LocalContext.current
    val isPositioning by viewModel.isPositioning.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Hero / Visual Header with wave background custom drawing
        val primaryColor = MaterialTheme.colorScheme.primary
        val secondaryColor = MaterialTheme.colorScheme.secondary
        val tertiaryColor = MaterialTheme.colorScheme.tertiary
        val waveColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .drawBehind {
                    val colors = listOf(tertiaryColor, secondaryColor, primaryColor)
                    val brush = Brush.verticalGradient(colors)
                    drawRect(brush)

                    // Draw decorative fishing waves
                    val wavePath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, size.height)
                        quadraticTo(
                            size.width * 0.25f, size.height - 40f,
                            size.width * 0.5f, size.height - 10f
                        )
                        quadraticTo(
                            size.width * 0.75f, size.height + 20f,
                            size.width, size.height - 30f
                        )
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(wavePath, waveColor)
                }
                .padding(20.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "爆护了吗",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "智慧钓况 • 科学守护 • 实战指南",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    // Geographic Spot Selector Chip
                    Card(
                        onClick = { 
                            if (!isPositioning) {
                                viewModel.triggerPreciseLocation(context, updateSelectedLocation = true)
                            }
                        },
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isPositioning) "精准定位中..." else selectedLocation.name,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Real-time Dynamic Weather Board for the location
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = getWeatherIcon(weatherCond),
                                contentDescription = null,
                                tint = Color(0xFFF4A261),
                                modifier = Modifier.size(28.dp)
                                    .padding(end = 6.dp)
                            )
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = selectedLocation.name,
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$weatherCond • $weatherTemp",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }

                        // Short advisory tag
                        Text(
                            text = if (weatherCond.contains("雨") || weatherCond.contains("云")) "🎯 活性高，钓浮截杀" else "☀️ 避暑钓深水区",
                            color = Color(0xFFE9C46A),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Search and Quick Filters
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Search Text Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("搜索鱼种、习性、分布...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // Category filter chips
            val filters = listOf("全部", "路亚鱼种", "手竿鱼种", "海竿鱼种")
            val context = LocalContext.current
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(filters) { filter ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { 
                            AnalyticsManager.trackEvent(context, AnalyticsManager.EVENT_FISH_FILTER, mapOf("filter" to filter))
                            viewModel.updateFilter(filter) 
                        },
                        label = { Text(filter) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        // Fish List
        if (fishList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "未找到符合条件的鱼类",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "试着更换搜索词或选择“全部”标签",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(fishList) { fish ->
                    FishItemCard(fish = fish, onClick = { onFishClick(fish) })
                }
            }
        }
    }


}

@Composable
fun FishItemCard(fish: Fish, onClick: () -> Unit) {
    val context = LocalContext.current
    Card(
        onClick = {
            AnalyticsManager.trackEvent(context, AnalyticsManager.EVENT_FISH_DETAIL, mapOf("fish_name" to fish.name))
            onClick()
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Fish Image Thumbnail loading with Coil, with offline vector-placeholder
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(fish.imageResId) // 使用本地资源 ID
                        .crossfade(true)
                        .build(),
                    contentDescription = fish.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = null // fall back to canvas drawing automatically
                )

                // Beautiful drawn abstract placeholder representing water & fish outline
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Draw soft waves and bubble decoration if load fails or image is empty
                    drawCircle(Color(0x330F4C5C), radius = 30f, center = Offset(size.width * 0.3f, size.height * 0.7f))
                    drawCircle(Color(0x220F4C5C), radius = 15f, center = Offset(size.width * 0.7f, size.height * 0.3f))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Info Content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = fish.name.substringBefore(" "),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Small category tag
                    val categoryColor = when (fish.category) {
                        "路亚" -> Color(0xFFE25822)
                        "手竿" -> Color(0xFF4F772D)
                        "海竿" -> Color(0xFF3A86C8)
                        else -> MaterialTheme.colorScheme.secondary
                    }
                    val categoryBg = when (fish.category) {
                        "路亚" -> Color(0xFFFCEADE)
                        "手竿" -> Color(0xFFE8F0E2)
                        "海竿" -> Color(0xFFE3F2FD)
                        else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                    }

                    Text(
                        text = fish.category,
                        color = categoryColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(
                                color = categoryBg,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = fish.scientificName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = fish.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Habitat indicator
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = fish.habitat,
                            fontSize = 10.sp,
                            color = Color(0xFF3A86C8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
