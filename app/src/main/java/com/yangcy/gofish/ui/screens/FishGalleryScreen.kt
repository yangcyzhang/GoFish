package com.yangcy.gofish.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yangcy.gofish.data.model.Fish
import com.yangcy.gofish.data.model.FishArticle
import com.yangcy.gofish.data.model.FishArticleData
import com.yangcy.gofish.data.model.FishData
import com.yangcy.gofish.ui.viewmodel.FishViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FishGalleryTab(
    viewModel: FishViewModel,
    onFishClick: (Fish) -> Unit
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("全部") }
    var gallerySearchQuery by remember { mutableStateOf("") }
    var selectedArticle by remember { mutableStateOf<FishArticle?>(null) }
    
    // Simple persistent bookmark state for the session
    val bookmarkedArticles = remember { mutableStateListOf<String>() }

    // Filter articles based on category and search query
    val filteredArticles = remember(selectedCategory, gallerySearchQuery) {
        var list = FishArticleData.articles
        if (selectedCategory != "全部") {
            list = list.filter { it.category == selectedCategory }
        }
        if (gallerySearchQuery.isNotBlank()) {
            list = list.filter {
                it.title.contains(gallerySearchQuery, ignoreCase = true) ||
                        it.summary.contains(gallerySearchQuery, ignoreCase = true) ||
                        it.contentMarkdown.contains(gallerySearchQuery, ignoreCase = true)
            }
        }
        list
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Hero Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "探索鱼类画廊",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "汇聚官方科普、名家作钓与生态守护文章",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Icon Decoration
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Article Search Bar
            OutlinedTextField(
                value = gallerySearchQuery,
                onValueChange = { gallerySearchQuery = it },
                placeholder = { Text("搜索画廊文章、知识点...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    if (gallerySearchQuery.isNotEmpty()) {
                        IconButton(onClick = { gallerySearchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            // Category list row
            val categories = listOf("全部", "钓鱼实战", "路亚运动", "生态守护")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = { Text(category, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = if (isSelected) null else FilterChipDefaults.filterChipBorder(
                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            enabled = true,
                            selected = false
                        )
                    )
                }
            }

            // Articles Main List
            if (filteredArticles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "未找到匹配的精彩文章",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                        if (gallerySearchQuery.isNotBlank()) {
                            TextButton(onClick = { gallerySearchQuery = "" }) {
                                Text("重置搜索条件")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredArticles) { article ->
                        val isBookmarked = bookmarkedArticles.contains(article.id)
                        GalleryArticleItem(
                            article = article,
                            isBookmarked = isBookmarked,
                            onBookmarkClick = {
                                if (isBookmarked) {
                                    bookmarkedArticles.remove(article.id)
                                    Toast.makeText(context, "已取消收藏", Toast.LENGTH_SHORT).show()
                                } else {
                                    bookmarkedArticles.add(article.id)
                                    Toast.makeText(context, "收藏成功！已放入我的书签", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onClick = { selectedArticle = article }
                        )
                    }
                }
            }
        }

        // Full Screen Article Reader Overlay
        AnimatedVisibility(
            visible = selectedArticle != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
        ) {
            selectedArticle?.let { article ->
                val isBookmarked = bookmarkedArticles.contains(article.id)
                ArticleReaderScreen(
                    article = article,
                    isBookmarked = isBookmarked,
                    onBookmarkToggle = {
                        if (isBookmarked) {
                            bookmarkedArticles.remove(article.id)
                            Toast.makeText(context, "已取消收藏", Toast.LENGTH_SHORT).show()
                        } else {
                            bookmarkedArticles.add(article.id)
                            Toast.makeText(context, "收藏成功！已放入我的书签", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onBack = { selectedArticle = null },
                    onFishClick = { relatedId ->
                        val matchedFish = FishData.fishList.find { it.id == relatedId }
                        if (matchedFish != null) {
                            selectedArticle = null
                            onFishClick(matchedFish)
                        } else {
                            Toast.makeText(context, "未找到对应的百科条目", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun GalleryArticleItem(
    article: FishArticle,
    isBookmarked: Boolean,
    onBookmarkClick: () -> Unit,
    onClick: () -> Unit
) {
    val categoryBadgeColor = when (article.category) {
        "钓鱼实战" -> MaterialTheme.colorScheme.primaryContainer
        "路亚运动" -> MaterialTheme.colorScheme.secondaryContainer
        "生态守护" -> Color(0xFFE8F5E9) // soft green
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val categoryTextColor = when (article.category) {
        "钓鱼实战" -> MaterialTheme.colorScheme.onPrimaryContainer
        "路亚运动" -> MaterialTheme.colorScheme.onSecondaryContainer
        "生态守护" -> Color(0xFF2E7D32)
        else -> MaterialTheme.colorScheme.onTertiaryContainer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Cover Image Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(article.coverImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = article.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Linear Gradient Overlay to make text and icons pop
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                            )
                        )
                )

                // Float Category Badge on Top-Left
                Surface(
                    color = categoryBadgeColor,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        text = article.category,
                        color = categoryTextColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Float Bookmark Icon on Top-Right
                IconButton(
                    onClick = onBookmarkClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Bookmark",
                        tint = if (isBookmarked) Color.Red else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Title and Meta floated at the bottom of the cover
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = article.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "作者: ${article.author}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "⏱️ ${article.readTime}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Summary description
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = article.summary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Read More Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "发布日期: ${article.publishDate}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "阅读全文",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleReaderScreen(
    article: FishArticle,
    isBookmarked: Boolean,
    onBookmarkToggle: () -> Unit,
    onBack: () -> Unit,
    onFishClick: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(article.category, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onBookmarkToggle) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) Color.Red else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Large Hero Header Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(article.coverImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = article.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
                )
                
                // Overlay text inside the cover
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = article.category,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = article.title,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 26.sp
                    )
                }
            }

            // Meta Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AccountBox, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = article.author, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(text = "爆护特约作者", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "发布：${article.publishDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "⏱️ 阅读需要 ${article.readTime}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Summary box
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "内容提要",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = article.summary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Article Body Text (parsed by double linebreaks for neat paragraph cards)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val paragraphs = article.contentMarkdown.split("\n\n")
                paragraphs.forEach { para ->
                    val trimmed = para.trim()
                    if (trimmed.startsWith("###")) {
                        // Section Header
                        val title = trimmed.replace("###", "").trim()
                        Text(
                            text = title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else if (trimmed.startsWith("-")) {
                        // List items
                        val itemsList = trimmed.split("\n")
                        itemsList.forEach { listItem ->
                            val cleanItem = listItem.replace("-", "").trim()
                            Row(modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 2.dp, bottom = 2.dp)) {
                                Text("• ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = cleanItem,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    } else {
                        // Regular text paragraph
                        Text(
                            text = trimmed,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // Related Fish Encyclopedia Link Button (CRITICAL INTERACTIVE FEATURE)
            if (article.relatedFishId != null) {
                val fishName = FishData.fishList.find { it.id == article.relatedFishId }?.name?.substringBefore(" ") ?: "鱼类"
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "💡 学无止境 • 对接百科图鉴",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "想了解关于『${fishName}』的详细科学参数、保护级别、竿组搭配吗？点击下方一键进入爆护百科图鉴进行深度比对。",
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { onFishClick(article.relatedFishId) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("一键前往『${fishName}』深度百科", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
