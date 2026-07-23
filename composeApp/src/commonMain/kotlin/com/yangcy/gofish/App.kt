package com.yangcy.gofish

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yangcy.gofish.ui.screens.MainScreen
import com.yangcy.gofish.ui.screens.PrivacyDialog
import com.yangcy.gofish.ui.viewmodel.FishViewModel
import gofish.composeapp.generated.resources.Res
import gofish.composeapp.generated.resources.ic_launcher_foreground
import org.jetbrains.compose.resources.painterResource

@Composable
fun App(
    viewModel: FishViewModel,
    initialPrivacyAccepted: Boolean,
    onPrivacyConfirm: () -> Unit,
    onPrivacyDismiss: () -> Unit
) {
    var accepted by remember { mutableStateOf(initialPrivacyAccepted) }
    var showSplash by remember { mutableStateOf(accepted) }

    LaunchedEffect(showSplash) {
        if (showSplash) {
            // 自动延迟 2.5 秒后跳转到主界面
            kotlinx.coroutines.delay(2500)
            showSplash = false
        }
    }

    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            if (showSplash) {
                SplashScreen(
                    onEnterClick = { showSplash = false }
                )
            } else {
                MainScreen(viewModel = viewModel)
            }
            
            if (!accepted) {
                PrivacyDialog(
                    onConfirm = {
                        accepted = true
                        onPrivacyConfirm()
                        showSplash = true // 同意隐私后进入开屏动画
                    },
                    onDismiss = onPrivacyDismiss
                )
            }
        }
    }
}

@Composable
fun SplashScreen(onEnterClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF071118))
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 顶部小字
            Text(
                text = "EXPLORE • RECORD • FORECAST",
                fontSize = 11.sp,
                color = Color(0xFF00ADB5),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 40.dp)
            )

            // 中部 Logo 及标题
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo 卡片
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1E2B)),
                    modifier = Modifier
                        .size(160.dp)
                        .border(1.5.dp, Color(0xFF00ADB5).copy(alpha = 0.2f), RoundedCornerShape(28.dp))
                        .shadow(12.dp, RoundedCornerShape(28.dp)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.ic_launcher_foreground),
                            contentDescription = "GoFish Logo",
                            modifier = Modifier.size(130.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                // 主标题
                Text(
                    text = "今天，你爆护了吗",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 副标题
                Text(
                    text = "专业级垂钓百科与气象实战助手",
                    fontSize = 14.sp,
                    color = Color(0xFF8E9AA5),
                    fontWeight = FontWeight.Medium
                )
            }

            // 底部按钮与自动跳转提示
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 40.dp)
            ) {
                Button(
                    onClick = onEnterClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00ADB5)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(52.dp)
                ) {
                    Text(
                        text = "立即开启爆护之旅",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF071118)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 自动跳转文字
                Text(
                    text = "自动跳转中...",
                    fontSize = 12.sp,
                    color = Color(0xFF5A6E7F)
                )
            }
        }
    }
}
