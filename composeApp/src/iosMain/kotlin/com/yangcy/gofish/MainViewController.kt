package com.yangcy.gofish

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.ComposeUIViewController
import com.yangcy.gofish.data.database.getDatabaseBuilder
import com.yangcy.gofish.data.repository.CatchRepository
import com.yangcy.gofish.data.repository.FishingSpotRepository
import com.yangcy.gofish.ui.viewmodel.FishViewModel
import com.yangcy.gofish.ui.viewmodel.IosLocationProvider
import platform.UIKit.UIViewController
import platform.UIKit.UIColor

import platform.Foundation.NSUserDefaults
import platform.posix.exit

class GofishAppKernel {
    val viewModel: FishViewModel by lazy {
        val db = getDatabaseBuilder().build()
        FishViewModel(
            CatchRepository(db.catchLogDao()),
            FishingSpotRepository(db.fishingSpotDao()),
            IosLocationProvider()
        )
    }

    fun createMainController(): UIViewController {
        val defaults = NSUserDefaults.standardUserDefaults
        val isPrivacyAccepted = defaults.boolForKey("privacy_accepted")

        return ComposeUIViewController(configure = {
            // 核心修复 1：禁用 Plist 检查，防止报错
            enforceStrictPlistSanityCheck = false
        }) {
            // 核心修复 2：最外层 Box 强制填色，确保其绘制在安全区之外
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF071118)) 
            ) {
                App(
                    viewModel = viewModel,
                    initialPrivacyAccepted = isPrivacyAccepted,
                    onPrivacyConfirm = {
                        defaults.setBool(true, forKey = "privacy_accepted")
                        defaults.synchronize()
                        viewModel.triggerPreciseLocation()
                    },
                    onPrivacyDismiss = {
                        exit(0)
                    }
                )
            }
        }
    }
}

fun MainViewController(): UIViewController = GofishAppKernel().createMainController()
