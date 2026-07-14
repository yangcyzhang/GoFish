package com.yangcy.gofish.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbCloudy
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yangcy.gofish.data.model.Fish
import com.yangcy.gofish.ui.viewmodel.FishViewModel
import org.jetbrains.compose.resources.painterResource

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
fun FishDetailScreen(
    fish: Fish,
    viewModel: FishViewModel,
    onBack: () -> Unit
) {
    val selectedLocation by viewModel.selectedLocation.collectAsState()
    val weatherCondition by viewModel.weatherCondition.collectAsState()
    val weatherTemp by viewModel.weatherTemp.collectAsState()
    val dynamicTip = viewModel.getDynamicFishingTip(fish)

    var activeTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text(fish.name.substringBefore(" "), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
            ) {
                Image(
                    painter = painterResource(fish.imageRes),
                    contentDescription = fish.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val brush = Brush.verticalGradient(listOf(Color.Transparent, Color(0x770F4C5C)))
                    drawRect(brush)
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = fish.scientificName,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEDF7F6)),
                border = BorderStroke(1.dp, Color(0xFF2A9D8F)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Favorite, null, tint = Color(0xFF2A9D8F), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("倡导文明垂钓、合理留放。幼鱼、孕鱼请放生，共护碧水微澜。", color = Color(0xFF1D5A51), fontSize = 11.sp)
                }
            }

            TabRow(selectedTabIndex = activeTab, containerColor = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.primary) {
                Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, text = { Text("习性特征", fontWeight = FontWeight.Bold) })
                Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, text = { Text("作钓秘籍", fontWeight = FontWeight.Bold) })
                Tab(selected = activeTab == 2, onClick = { activeTab = 2 }, text = { Text("气象与装备", fontWeight = FontWeight.Bold) })
            }

            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                when (activeTab) {
                    0 -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        DetailSectionCard("精准分辨与外形特征", Icons.Default.Search, fish.description)
                        DetailSectionCard("栖息规律", Icons.Default.Info, fish.habitat)
                        DetailSectionCard("觅食规律", Icons.Default.Star, fish.feedingHabit)
                        DetailSectionCard("分布区域", Icons.Default.LocationOn, fish.distribution)
                    }
                    1 -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        DetailSectionCard("实战作钓要领", Icons.Default.List, fish.anglingStrategy)
                        Card(shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("推荐钓具基础搭配", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp))
                                GearRow(Icons.Default.Settings, "推荐钓竿", fish.rodRecommendation)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                GearRow(Icons.Default.Build, "推荐线组", fish.lineRecommendation)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                GearRow(Icons.Default.Star, "高效饵料", fish.baitRecommendation)
                            }
                        }
                    }
                    2 -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("实时钓况装备智能研判", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    WeatherParamIndicator("钓点", selectedLocation.name.substringBefore(" "), Icons.Default.LocationOn)
                                    WeatherParamIndicator("天气", weatherCondition, getWeatherIcon(weatherCondition))
                                    WeatherParamIndicator("温度", weatherTemp, Icons.Outlined.Thermostat)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), RoundedCornerShape(12.dp)).padding(12.dp)) {
                                    Text(dynamicTip, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        DetailSectionCard("通用气象偏好指南", Icons.Default.Info, fish.defaultWeatherTips)
                    }
                }
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun GearRow(icon: ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DetailSectionCard(title: String, icon: ImageVector, content: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            }
            Text(content, fontSize = 13.sp, lineHeight = 19.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun WeatherParamIndicator(label: String, value: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}
