package com.yangcy.gofish.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yangcy.gofish.ui.viewmodel.formatDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnglerToolboxScreen() {
    var activeSubTool by remember { mutableStateOf(0) } // 0: 估重, 1: 气压, 2: 容线

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
    ) {
        // --- 头部渐变标题栏 ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0066A1), Color(0xFF004D73))
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Text(
                text = "钓鱼百宝箱",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black
            )
        }

        // --- 顶部选项卡菜单 ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val subTools = listOf("渔获估重", "气压出钓", "线轮容量")
            subTools.forEachIndexed { index, title ->
                val isSelected = activeSubTool == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFF0066A1) else Color.Transparent)
                        .clickable { activeSubTool = index }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) Color.White else Color.DarkGray,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // --- 主内容区域（根据选项卡渲染） ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            when (activeSubTool) {
                0 -> FishWeightEstimatorWidget()
                1 -> BarometerFishingWidget()
                2 -> SpoolCapacityWidget()
            }
        }
    }
}

// ==========================================
// 1. 渔获估重计算器 (Fish Weight Estimator)
// ==========================================
@Composable
fun FishWeightEstimatorWidget() {
    val speciesList = listOf("鲈鱼 (Bass)", "鲤/草鱼 (Carp)", "罗非鱼 (Tilapia)", "通用鱼种 (Generic)")
    var selectedSpecies by remember { mutableStateOf("鲈鱼 (Bass)") }
    var length by remember { mutableStateOf(45f) } // 长度 (cm)
    var girth by remember { mutableStateOf(30f) }  // 周长/腰围 (cm)

    // 基于公式进行重量估算 (克/千克转换)
    val estimatedWeight = remember(selectedSpecies, length, girth) {
        val factor = when (selectedSpecies) {
            "鲈鱼 (Bass)" -> 28000.0
            "鲤/草鱼 (Carp)" -> 29500.0
            "罗非鱼 (Tilapia)" -> 26000.0
            else -> 28900.0
        }
        val weightKg = (length * girth * girth) / factor
        weightKg
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "估算设定",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF0066A1)
                )

                // 鱼种选择
                Column {
                    Text("鱼种选择", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        speciesList.take(2).forEach { species ->
                            val isSelected = selectedSpecies == species
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFFE3F2FD) else Color(0xFFF5F7FA))
                                    .border(1.dp, if (isSelected) Color(0xFF0066A1) else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { selectedSpecies = species }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(species.substringBefore(" "), fontSize = 12.sp, color = if (isSelected) Color(0xFF0066A1) else Color.DarkGray)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        speciesList.drop(2).forEach { species ->
                            val isSelected = selectedSpecies == species
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFFE3F2FD) else Color(0xFFF5F7FA))
                                    .border(1.dp, if (isSelected) Color(0xFF0066A1) else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { selectedSpecies = species }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(species.substringBefore(" "), fontSize = 12.sp, color = if (isSelected) Color(0xFF0066A1) else Color.DarkGray)
                            }
                        }
                    }
                }

                Divider(color = Color(0xFFEEEEEE))

                // 长度滑块
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("鱼体全长 (L)", fontSize = 12.sp, color = Color.Gray)
                        Text("${length.toInt()} cm", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0066A1))
                    }
                    Slider(
                        value = length,
                        onValueChange = { length = it },
                        valueRange = 10f..120f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF0066A1), activeTrackColor = Color(0xFF0066A1))
                    )
                }

                // 周长滑块
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("鱼体最厚处周长 (G)", fontSize = 12.sp, color = Color.Gray)
                        Text("${girth.toInt()} cm", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0066A1))
                    }
                    Slider(
                        value = girth,
                        onValueChange = { girth = it },
                        valueRange = 5f..80f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF0066A1), activeTrackColor = Color(0xFF0066A1))
                    )
                }
            }
        }

        // 估重结果卡片
        Card(
            modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "估算重量结果",
                    fontSize = 13.sp,
                    color = Color(0xFF0066A1),
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = formatDecimal(estimatedWeight),
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0066A1)
                    )
                    Text(
                        text = " 公斤 (kg)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0066A1),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                val lbs = estimatedWeight * 2.20462
                Text(
                    text = "相当于约 ${formatDecimal(lbs)} 磅 / ${formatDecimal(estimatedWeight * 2.0)} 市斤",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "💡 物理算理：此公式根据水生动物体积系数反推计算。建议使用卷尺测量鱼体最厚实位置的周长，可以提高90%以上的精确度。",
                        fontSize = 11.sp,
                        color = Color(0xFF555555),
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

// ==========================================
// 2. 黄金出钓气压指数 (Barometer Fishing Index)
// ==========================================
@Composable
fun BarometerFishingWidget() {
    var pressure by remember { mutableStateOf(1013f) } // hPa

    val activityState = remember(pressure) {
        when {
            pressure >= 1012f && pressure <= 1018f -> Triple("极佳 (Excellent)", Color(0xFF4CAF50), "气压非常稳定偏高，水体溶氧量处于黄金区间，鱼情极其活跃，咬钩凶猛，推荐出钓爆护！")
            pressure in 1006f..1011f || pressure in 1019f..1023f -> Triple("适宜 (Good)", Color(0xFF0066A1), "当前气压条件正常，水温水氧处于温和阶段。鱼口稳定，是正常出钓的好天气。")
            pressure < 1000f -> Triple("不宜 (Poor)", Color(0xFFE53935), "极低气压！天气可能处于暴雨、雷闷状态。水体严重缺氧，鱼口近乎完全闭口，极易空军。")
            else -> Triple("一般 (Neutral)", Color(0xFFFFB300), "气压偏低或过高，鱼儿活性有所下降。建议选择背风、深水区域作钓。")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "气压条件调整",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF0066A1)
                )

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("当前环境气压值", fontSize = 12.sp, color = Color.Gray)
                        Text("${pressure.toInt()} hPa", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0066A1))
                    }
                    Slider(
                        value = pressure,
                        onValueChange = { pressure = it },
                        valueRange = 980f..1040f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF0066A1), activeTrackColor = Color(0xFF0066A1))
                    )
                }
            }
        }

        // 状态面板
        Card(
            modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("出钓指数判定", fontSize = 12.sp, color = Color.Gray)

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(activityState.second.copy(alpha = 0.15f))
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = activityState.first,
                        color = activityState.second,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = activityState.third,
                    fontSize = 13.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

// ==========================================
// 3. 线轮容线量换算器 (Spool Capacity Calculator)
// ==========================================
@Composable
fun SpoolCapacityWidget() {
    val spoolSizes = listOf("1000型", "2000型", "2500型", "3000型", "4000型")
    var selectedSpool by remember { mutableStateOf("2000型") }
    
    val lineTypes = listOf("尼龙线 (Nylon)", "PE编织线 (PE)", "碳氟线 (Fluorocarbon)")
    var selectedLineType by remember { mutableStateOf("PE编织线 (PE)") }
    
    val lineSizes = listOf("0.4#", "0.6#", "0.8#", "1.0#", "1.5#", "2.0#", "3.0#", "4.0#")
    var selectedLineSize by remember { mutableStateOf("1.0#") }

    // 经典线直径与物理截面积算法
    val lineDiameters = mapOf(
        "0.4#" to 0.104,
        "0.6#" to 0.128,
        "0.8#" to 0.148,
        "1.0#" to 0.165,
        "1.5#" to 0.203,
        "2.0#" to 0.235,
        "3.0#" to 0.285,
        "4.0#" to 0.330
    )

    // 线杯基准容纳体积（参考线径截面积）
    val referenceCapacity = when (selectedSpool) {
        "1000型" -> 100.0 * (0.165 * 0.165) // 基准：100m of 1.0# line
        "2000型" -> 150.0 * (0.165 * 0.165) // 基准：150m of 1.0# line
        "2500型" -> 150.0 * (0.203 * 0.203) // 基准：150m of 1.5# line
        "3000型" -> 150.0 * (0.235 * 0.235) // 基准：150m of 2.0# line
        else -> 150.0 * (0.285 * 0.285)     // 基准：150m of 3.0# line
    }

    // 计算容线长度
    val calculatedMeters = remember(selectedSpool, selectedLineType, selectedLineSize) {
        val targetDiameter = lineDiameters[selectedLineSize] ?: 0.165
        val baseMeters = referenceCapacity / (targetDiameter * targetDiameter)
        
        // 不同线材质地和蓬松度修正系数 (PE蓬松且有空隙，折算88%；尼龙较硬，折算95%)
        val factor = when {
            selectedLineType.contains("PE") -> 0.88
            selectedLineType.contains("碳氟") -> 0.92
            else -> 0.95
        }
        (baseMeters * factor).toInt()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "线轮参数选择",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF0066A1)
                )

                // 1. 线轮杯型
                Column {
                    Text("线轮型号/杯深", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        spoolSizes.take(3).forEach { sizeOption ->
                            val isSelected = selectedSpool == sizeOption
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFFE3F2FD) else Color(0xFFF5F7FA))
                                    .border(1.dp, if (isSelected) Color(0xFF0066A1) else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { selectedSpool = sizeOption }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(sizeOption, fontSize = 12.sp, color = if (isSelected) Color(0xFF0066A1) else Color.DarkGray)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        spoolSizes.drop(3).forEach { sizeOption ->
                            val isSelected = selectedSpool == sizeOption
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFFE3F2FD) else Color(0xFFF5F7FA))
                                    .border(1.dp, if (isSelected) Color(0xFF0066A1) else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { selectedSpool = sizeOption }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(sizeOption, fontSize = 12.sp, color = if (isSelected) Color(0xFF0066A1) else Color.DarkGray)
                            }
                        }
                        Box(modifier = Modifier.weight(1f)) // 对齐占位
                    }
                }

                Divider(color = Color(0xFFEEEEEE))

                // 2. 钓线种类
                Column {
                    Text("钓线材质", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        lineTypes.forEach { typeOption ->
                            val isSelected = selectedLineType == typeOption
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFFE3F2FD) else Color(0xFFF5F7FA))
                                    .border(1.dp, if (isSelected) Color(0xFF0066A1) else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { selectedLineType = typeOption }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(typeOption.substringBefore(" "), fontSize = 11.sp, color = if (isSelected) Color(0xFF0066A1) else Color.DarkGray)
                            }
                        }
                    }
                }

                Divider(color = Color(0xFFEEEEEE))

                // 3. 钓线线号
                Column {
                    Text("选定线号 (号数)", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            lineSizes.take(4).forEach { sizeOption ->
                                val isSelected = selectedLineSize == sizeOption
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFFE3F2FD) else Color(0xFFF5F7FA))
                                        .border(1.dp, if (isSelected) Color(0xFF0066A1) else Color.Transparent, RoundedCornerShape(8.dp))
                                        .clickable { selectedLineSize = sizeOption }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(sizeOption, fontSize = 12.sp, color = if (isSelected) Color(0xFF0066A1) else Color.DarkGray)
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            lineSizes.drop(4).forEach { sizeOption ->
                                val isSelected = selectedLineSize == sizeOption
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFFE3F2FD) else Color(0xFFF5F7FA))
                                        .border(1.dp, if (isSelected) Color(0xFF0066A1) else Color.Transparent, RoundedCornerShape(8.dp))
                                        .clickable { selectedLineSize = sizeOption }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(sizeOption, fontSize = 12.sp, color = if (isSelected) Color(0xFF0066A1) else Color.DarkGray)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 容线计算结果
        Card(
            modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "预计可缠绕长度",
                    fontSize = 13.sp,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = calculatedMeters.toString(),
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF2E7D32)
                    )
                    Text(
                        text = " 米 (meters)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                val currentDiameter = lineDiameters[selectedLineSize] ?: 0.165
                Text(
                    text = "当前线号基准线径: ${currentDiameter} mm",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
    }
}
