package com.yangcy.gofish.ui.screens

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.yangcy.gofish.data.model.Fish
import com.yangcy.gofish.data.model.FishArticle
import com.yangcy.gofish.data.model.FishArticleData
import com.yangcy.gofish.data.model.FishData
import com.yangcy.gofish.ui.viewmodel.FishViewModel
import com.yangcy.gofish.util.showToast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FishGalleryTab(
    viewModel: FishViewModel,
    onFishClick: (Fish) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("全部") }
    var gallerySearchQuery by remember { mutableStateOf("") }
    var selectedArticle by remember { mutableStateOf<FishArticle?>(null) }
    
    val bookmarkedArticles = remember { mutableStateListOf<String>() }

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
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f), MaterialTheme.colorScheme.background))).padding(horizontal = 16.dp, vertical = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("探索鱼类画廊", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        Text("汇聚官方科普、名家作钓与生态守护文章", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), modifier = Modifier.size(48.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }

            OutlinedTextField(
                value = gallerySearchQuery,
                onValueChange = { gallerySearchQuery = it },
                placeholder = { Text("搜索画廊文章...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(24.dp),
                singleLine = true
            )

            val categories = listOf("全部", "钓鱼实战", "路亚运动", "海竿进阶")
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { category ->
                    val isSelected = selectedCategory == category
                    FilterChip(selected = isSelected, onClick = { selectedCategory = category }, label = { Text(category, fontSize = 12.sp) })
                }
            }

            if (filteredArticles.isEmpty()) {
                // Empty state
            } else {
                LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f)) {
                    items(filteredArticles) { article ->
                        val isBookmarked = bookmarkedArticles.contains(article.id)
                        GalleryArticleItem(
                            article = article,
                            isBookmarked = isBookmarked,
                            onBookmarkClick = {
                                if (isBookmarked) {
                                    bookmarkedArticles.remove(article.id)
                                    showToast("已取消收藏")
                                } else {
                                    bookmarkedArticles.add(article.id)
                                    showToast("收藏成功")
                                }
                            },
                            onClick = { selectedArticle = article }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = selectedArticle != null, enter = fadeIn(), exit = fadeOut()) {
            selectedArticle?.let { article ->
                val isBookmarked = bookmarkedArticles.contains(article.id)
                ArticleReaderScreen(
                    article = article,
                    isBookmarked = isBookmarked,
                    onBookmarkToggle = {
                        if (isBookmarked) {
                            bookmarkedArticles.remove(article.id)
                            showToast("已取消收藏")
                        } else {
                            bookmarkedArticles.add(article.id)
                            showToast("收藏成功")
                        }
                    },
                    onBack = { selectedArticle = null },
                    onFishClick = { relatedId ->
                        val matchedFish = FishData.fishList.find { it.id == relatedId }
                        if (matchedFish != null) {
                            selectedArticle = null
                            onFishClick(matchedFish)
                        } else {
                            showToast("未找到百科条目")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun GalleryArticleItem(article: FishArticle, isBookmarked: Boolean, onBookmarkClick: () -> Unit, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(16.dp)) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                AsyncImage(model = article.coverImageUrl, contentDescription = article.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                IconButton(onClick = onBookmarkClick, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape).size(36.dp)) {
                    Icon(imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null, tint = if (isBookmarked) Color.Red else Color.White, modifier = Modifier.size(18.dp))
                }
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                    Text(article.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(article.summary, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleReaderScreen(article: FishArticle, isBookmarked: Boolean, onBookmarkToggle: () -> Unit, onBack: () -> Unit, onFishClick: (String) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(article.category, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = { IconButton(onClick = onBookmarkToggle) { Icon(if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (isBookmarked) Color.Red else MaterialTheme.colorScheme.onSurface) } }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState())) {
            Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                AsyncImage(model = article.coverImageUrl, contentDescription = article.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(article.title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(16.dp))
                Text(article.contentMarkdown, fontSize = 14.sp, lineHeight = 22.sp)
                
                if (article.relatedFishId != null) {
                    Button(onClick = { onFishClick(article.relatedFishId) }, modifier = Modifier.padding(top = 16.dp)) {
                        Text("前往深度百科")
                    }
                }
            }
        }
    }
}
