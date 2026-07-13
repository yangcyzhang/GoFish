package com.yangcy.gofish

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.umeng.commonsdk.UMConfigure
import com.umeng.analytics.MobclickAgent
import com.umeng.umcrash.UMCrash
import com.yangcy.gofish.ui.screens.MainScreen
import com.yangcy.gofish.ui.screens.PrivacyDialog
import com.yangcy.gofish.ui.theme.MyApplicationTheme
import com.yangcy.gofish.ui.viewmodel.FishViewModel

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        
        if (fineGranted || coarseGranted) {
            Log.d("MainActivity", "Location permissions granted.")
        } else {
            Log.w("MainActivity", "Location permissions denied.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sharedPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isPrivacyAccepted = sharedPrefs.getBoolean("privacy_accepted", false)

        if (isPrivacyAccepted) {
            initSdkCompliance()
            checkLocationPermissions()
        }

        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                var privacyAccepted by remember { mutableStateOf(isPrivacyAccepted) }
                val viewModel: FishViewModel = viewModel()
                
                Box {
                    if (privacyAccepted) {
                        MainScreen(viewModel = viewModel)
                    } else {
                        // Background placeholder while showing privacy dialog
                        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {}
                        
                        PrivacyDialog(
                            onConfirm = {
                                sharedPrefs.edit().putBoolean("privacy_accepted", true).apply()
                                initSdkCompliance()
                                checkLocationPermissions()
                                // Trigger initial location fix now that we have consent
                                viewModel.triggerPreciseLocation(this@MainActivity, showToast = false)
                                privacyAccepted = true
                            },
                            onDismiss = {
                                finish()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun initSdkCompliance() {
        try {
            // AMap 正式初始化 (同意隐私)
            com.amap.api.maps.MapsInitializer.setApiKey(BuildConfig.AMAP_API_KEY)
            com.amap.api.maps.MapsInitializer.updatePrivacyAgree(this, true)
            com.amap.api.location.AMapLocationClient.updatePrivacyAgree(this, true)
            
            // 友盟正式初始化 (采集数据)
            UMConfigure.init(this, BuildConfig.UMENG_APP_KEY, "Umeng", UMConfigure.DEVICE_TYPE_PHONE, "")
            UMCrash.init(this, BuildConfig.UMENG_APP_KEY, "Umeng")
            MobclickAgent.setPageCollectionMode(MobclickAgent.PageMode.LEGACY_MANUAL)
        } catch (e: SecurityException) {
            Log.e("MainActivity", "SecurityException during SDK init (WRITE_SETTINGS blocked by system)", e)
        } catch (e: Exception) {
            Log.e("MainActivity", "Compliance Init Error", e)
        }
    }

    override fun onResume() {
        super.onResume()
        // 合规要求：未同意隐私前严禁调用此接口
        val sharedPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        if (sharedPrefs.getBoolean("privacy_accepted", false)) {
            MobclickAgent.onResume(this)
        }
    }

    override fun onPause() {
        super.onPause()
        val sharedPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        if (sharedPrefs.getBoolean("privacy_accepted", false)) {
            MobclickAgent.onPause(this)
        }
    }

    private fun checkLocationPermissions() {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineLocationGranted && !coarseLocationGranted) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
}
