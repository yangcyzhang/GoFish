package com.yangcy.gofish.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.yangcy.gofish.ui.viewmodel.FishViewModel

@Composable
actual fun FishingMapScreen(viewModel: FishViewModel) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("iOS Map Integration coming soon")
    }
}
