package com.yangcy.gofish.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbCloudy
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yangcy.gofish.data.model.Fish
import com.yangcy.gofish.ui.viewmodel.FishViewModel
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.Image

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
    var currentTab by remember { mutableStateOf(1) } 
    var selectedFish by remember { mutableStateOf<Fish?>(null) }
    var showAddCatchDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (selectedFish != null) {
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
                            Triple("钓点地图", Icons.Default.LocationOn, 0),
                            Triple("鱼类图鉴", Icons.Default.Home, 1),
                            Triple("渔获日记", Icons.Default.List, 2),
                            Triple("钓鱼工具", Icons.Default.Widgets, 3)
                        )
                        tabs.forEach { (label, filledIcon, index) ->
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
                    when (currentTab) {
                        0 -> FishingMapScreen(viewModel = viewModel)
                        1 -> FishCatalogTab(viewModel = viewModel, onFishClick = { selectedFish = it })
                        2 -> CatchHistoryTab(viewModel = viewModel, onAddClick = { showAddCatchDialog = true })
                        3 -> AnglerToolboxScreen()
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

    Column(modifier = Modifier.fillMaxSize()) {
        val primaryColor = MaterialTheme.colorScheme.primary
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .background(primaryColor)
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
                        onClick = { viewModel.updateFilter(filter) },
                        label = { Text(filter) }
                    )
                }
            }
        }

        if (fishList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无数据", color = Color.Gray)
            }
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
        onClick = { onClick() },
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
                }
                Text(fish.scientificName, fontSize = 11.sp, fontWeight = FontWeight.Light, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(6.dp))
                Text(fish.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
