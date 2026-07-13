package com.yangcy.gofish.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yangcy.gofish.data.model.CatchLog
import com.yangcy.gofish.data.model.FishData
import com.yangcy.gofish.ui.viewmodel.FishViewModel
import com.yangcy.gofish.util.AnalyticsManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CatchHistoryTab(
    viewModel: FishViewModel,
    onAddClick: () -> Unit
) {
    val catches by viewModel.catchLogs.collectAsState()

    // Calculate dynamic stats
    val totalCatches = catches.size
    val maxWeight = if (catches.isEmpty()) 0.0 else catches.maxOf { it.weightKg }
    val favoriteBait = if (catches.isEmpty()) "无" else {
        catches.groupBy { it.baitUsed }
            .maxByOrNull { it.value.size }?.key ?: "无"
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Stats Summary Dashboard - Stretched to full width
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(0.dp), // Squared to bleed into edges
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "我的爆护渔绩",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "作钓生涯日记",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatBox("起鱼总数", "$totalCatches 尾")
                    StatBox("最大渔获", "${String.format(Locale.getDefault(), "%.1f", maxWeight)} kg")
                    StatBox("黄金用饵", favoriteBait)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "渔获记录时间轴",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("记一笔", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Log List
        if (catches.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "暂无渔获记录",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "点击右上角“记一笔”添加你的第一条钓鱼日志吧！",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                items(catches) { log ->
                    val context = LocalContext.current
                    CatchLogItem(log = log, onDelete = { 
                        AnalyticsManager.trackEvent(context, AnalyticsManager.EVENT_DELETE_CATCH)
                        viewModel.deleteCatchLog(log) 
                    })
                }
            }
        }
    }
}

@Composable
fun CatchLogItem(log: CatchLog, onDelete: () -> Unit) {
    val dateStr = remember(log.timestamp) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        sdf.format(Date(log.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = log.fishName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = log.location,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${log.weightKg} kg / ${log.lengthCm} cm",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.6f))
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "用饵: ${log.baitUsed}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF52B788), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${log.weatherCondition} • ${log.temperature}", fontSize = 11.sp, color = Color.Gray)
                }
            }

            if (log.notes.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = log.notes,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Text(
                text = dateStr,
                fontSize = 9.sp,
                color = Color.LightGray,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun StatBox(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.White)
        Text(text = label, fontSize = 9.sp, color = Color.White.copy(alpha = 0.7f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCatchDialog(
    viewModel: FishViewModel,
    onDismiss: () -> Unit
) {
    val selectedLocation by viewModel.selectedLocation.collectAsState()

    var fishName by remember { mutableStateOf("鲫鱼") }
    var isCustomFish by remember { mutableStateOf(false) }
    var customFishName by remember { mutableStateOf("") }
    
    var locationInput by remember { mutableStateOf(selectedLocation.name.substringBefore(" ")) }
    var weightInput by remember { mutableStateOf("0.5") }
    var lengthInput by remember { mutableStateOf("22") }
    var baitInput by remember { mutableStateOf("红虫") }
    var notesInput by remember { mutableStateOf("") }

    val fishPresets = listOf("鲫鱼", "黄脚立", "草鱼","石九公","罗非", "翘嘴","石斑","鳜鱼", "鲤鱼", "鲶鱼", "黑鱼")

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            val context = LocalContext.current
            Button(
                onClick = {
                    AnalyticsManager.trackEvent(context, AnalyticsManager.EVENT_ADD_CATCH)
                    val finalFishName = if (isCustomFish) customFishName else fishName
                    val weight = weightInput.toDoubleOrNull() ?: 0.5
                    val length = lengthInput.toDoubleOrNull() ?: 20.0
                    viewModel.addCatchLog(
                        fishName = finalFishName,
                        location = locationInput,
                        weight = weight,
                        length = length,
                        bait = baitInput,
                        notes = notesInput
                    )
                    onDismiss()
                }
            ) {
                Text("保存日记")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.List, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("新增渔获记录", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Dropdown mock for Fish Name selection
                Text("选择或输入鱼种:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                ScrollableRowSelector(
                    items = fishPresets + "自定义",
                    selectedItem = if (isCustomFish) "自定义" else fishName,
                    onSelect = { 
                        if (it == "自定义") {
                            isCustomFish = true
                        } else {
                            isCustomFish = false
                            fishName = it
                        }
                    }
                )

                if (isCustomFish) {
                    OutlinedTextField(
                        value = customFishName,
                        onValueChange = { customFishName = it },
                        label = { Text("输入自定义鱼种") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = locationInput,
                    onValueChange = { locationInput = it },
                    label = { Text("作钓水域/地点") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text("重量 (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = lengthInput,
                        onValueChange = { lengthInput = it },
                        label = { Text("长度 (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = baitInput,
                    onValueChange = { baitInput = it },
                    label = { Text("作钓用饵") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("作钓心得/日记备注") },
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

@Composable
fun ScrollableRowSelector(
    items: List<String>,
    selectedItem: String,
    onSelect: (String) -> Unit
) {
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(items) { item ->
            val isSelected = item == selectedItem
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(item) },
                label = { Text(item, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}
