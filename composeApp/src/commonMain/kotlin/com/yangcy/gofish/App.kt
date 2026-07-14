package com.yangcy.gofish

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.yangcy.gofish.ui.screens.MainScreen
import com.yangcy.gofish.ui.screens.PrivacyDialog
import com.yangcy.gofish.ui.theme.MyApplicationTheme
import com.yangcy.gofish.ui.viewmodel.FishViewModel

@Composable
fun App(
    viewModel: FishViewModel,
    initialPrivacyAccepted: Boolean,
    onPrivacyConfirm: () -> Unit,
    onPrivacyDismiss: () -> Unit
) {
    var privacyAccepted by remember { mutableStateOf(initialPrivacyAccepted) }

    MyApplicationTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (privacyAccepted) {
                    MainScreen(viewModel = viewModel)
                } else {
                    // Background placeholder while showing privacy dialog
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {}
                    
                    PrivacyDialog(
                        onConfirm = {
                            privacyAccepted = true
                            onPrivacyConfirm()
                        },
                        onDismiss = onPrivacyDismiss
                    )
                }
            }
        }
    }
}
