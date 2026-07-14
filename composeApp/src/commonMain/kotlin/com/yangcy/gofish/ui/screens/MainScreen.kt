package com.yangcy.gofish.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yangcy.gofish.data.model.Fish
import com.yangcy.gofish.ui.viewmodel.FishViewModel
import com.yangcy.gofish.util.analytics
import com.yangcy.gofish.util.showToast
import org.jetbrains.compose.resources.painterResource
import gofish.composeapp.generated.resources.Res
import gofish.composeapp.generated.resources.ic_launcher_foreground

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
    var currentTab by remember { mutableStateOf(0) } // 0: 钓点地图, 1: 图鉴, 2: 渔获记录
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
            // Track Fish Detail Page
            DisposableEffect(selectedFish) {
                analytics.onPageStart("FishDetail_${selectedFish?.name ?: "Unknown"}")
                onDispose {
                    analytics.onPageEnd("FishDetail_${selectedFish?.name ?: "Unknown"}")
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
                        tabs.forEachIndexed { index, (label, filledIcon, _) ->
                            val isSelected = currentTab == index
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { currentTab = index },
                                icon = {
                                    Icon(
                                        imageVector = filledIcon,
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
                    Box(modifier = Modifier.fillMaxSize().alpha(if (currentTab == 0) 1f else 0f)) {
                        // FishingMapScreen will be called here
                        FishingMapScreen(viewModel = viewModel)
                        if (currentTab != 0) {
                            Box(modifier = Modifier.fillMaxSize().clickable(enabled = false) {})
                        }
                    }

                    DisposableEffect(currentTab) {
                        val pageName = when(currentTab) {
                            0 -> "FishingMap"
                            1 -> "FishCatalog"
                            2 -> "CatchHistory"
                            else -> "Unknown"
                        }
                        analytics.onPageStart(pageName)
                        analytics.trackEvent("tab_switch", mapOf("tab_index" to currentTab))
                        onDispose {
                            analytics.onPageEnd(pageName)
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

        // Splash Screen Overlay
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .systemBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "EXPLORE • RECORD • FORECAST",
                        color = Color(0xFF00ADB5).copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(top = 40.dp)
                    )

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
                            Image(
                                painter = painterResource(Res.drawable.ic_launcher_foreground),
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

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 30.dp)
                    ) {
                        Button(
                            onClick = { 
                                analytics.trackEvent("splash_enter_click")
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
    val weatherTemp by viewModel.weatherTemp.collectAsState()
    val weatherCond by viewModel.weatherCondition.collectAsState()
    val isPositioning by viewModel.isPositioning.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
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

                    Card(
                        onClick = { 
                            if (!isPositioning) {
                                viewModel.triggerPreciseLocation(
                                    onFrequentRequest = { showToast("请求过于频繁") },
                                    onResult = { name -> showToast("定位成功: $name") }
                                )
                            }
                        },
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LocationOn, null, tint = Color.White, modifier = Modifier.size(16.dp))
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

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(getWeatherIcon(weatherCond), null, tint = Color(0xFFF4A261), modifier = Modifier.size(28.dp).padding(end = 6.dp))
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(selectedLocation.name, fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("$weatherCond • $weatherTemp", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                            }
                        }
                        Text(
                            text = if (weatherCond.contains("雨") || weatherCond.contains("云")) "🎯 活性高，钓浮截杀" else "☀️ 避暑钓深水区",
                            color = Color(0xFFE9C46A),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("搜索鱼种、习性、分布...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                shape = RoundedCornerShape(24.dp),
                singleLine = true
            )

            val filters = listOf("全部", "路亚鱼种", "手竿鱼种", "海竿鱼种")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                items(filters) { filter ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { 
                            analytics.trackEvent("fish_catalog_filter", mapOf("filter" to filter))
                            viewModel.updateFilter(filter) 
                        },
                        label = { Text(filter) }
                    )
                }
            }
        }

        if (fishList.isEmpty()) {
            // Empty state
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
                items(fishList) { fish ->
                    FishItemCard(fish = fish, onClick = { onFishClick(fish) })
                }
            }
        }
    }
}

@Composable
fun FishItemCard(fish: Fish, onClick: () -> Unit) {
    Card(
        onClick = {
            analytics.trackEvent("view_fish_detail", mapOf("fish_name" to fish.name))
            onClick()
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))) {
                Image(
                    painter = painterResource(fish.imageRes),
                    contentDescription = fish.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(fish.name.substringBefore(" "), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = fish.category,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(fish.scientificName, fontSize = 11.sp, fontWeight = FontWeight.Light, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(6.dp))
                Text(fish.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(8.dp))
                Text(fish.habitat, fontSize = 10.sp, color = Color(0xFF3A86C8), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
