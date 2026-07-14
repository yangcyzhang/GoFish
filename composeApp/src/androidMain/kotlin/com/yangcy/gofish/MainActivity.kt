package com.yangcy.gofish

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.umeng.analytics.MobclickAgent
import com.umeng.commonsdk.UMConfigure
import com.umeng.umcrash.UMCrash
import com.yangcy.gofish.data.database.getDatabaseBuilder
import com.yangcy.gofish.data.repository.CatchRepository
import com.yangcy.gofish.data.repository.FishingSpotRepository
import com.yangcy.gofish.ui.viewmodel.AndroidLocationProvider
import com.yangcy.gofish.ui.viewmodel.FishViewModel

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (fineGranted) {
            Log.d("MainActivity", "Location permissions granted.")
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
            val db = remember { getDatabaseBuilder().build() }
            val catchRepo = remember { CatchRepository(db.catchLogDao()) }
            val spotRepo = remember { FishingSpotRepository(db.fishingSpotDao()) }
            val locationProvider = remember { AndroidLocationProvider(this) }
            
            val viewModel: FishViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return FishViewModel(catchRepo, spotRepo, locationProvider) as T
                }
            })
            
            App(
                viewModel = viewModel,
                initialPrivacyAccepted = isPrivacyAccepted,
                onPrivacyConfirm = {
                    sharedPrefs.edit().putBoolean("privacy_accepted", true).apply()
                    initSdkCompliance()
                    checkLocationPermissions()
                    viewModel.triggerPreciseLocation()
                },
                onPrivacyDismiss = {
                    finish()
                }
            )
        }
    }

    private fun initSdkCompliance() {
        try {
            com.amap.api.maps.MapsInitializer.setApiKey(BuildConfig.AMAP_API_KEY)
            com.amap.api.maps.MapsInitializer.updatePrivacyAgree(this, true)
            com.amap.api.location.AMapLocationClient.updatePrivacyAgree(this, true)
            
            UMConfigure.init(this, BuildConfig.UMENG_APP_KEY, "Umeng", UMConfigure.DEVICE_TYPE_PHONE, "")
            UMCrash.init(this, BuildConfig.UMENG_APP_KEY, "Umeng")
            MobclickAgent.setPageCollectionMode(MobclickAgent.PageMode.LEGACY_MANUAL)
        } catch (e: SecurityException) {
            Log.e("MainActivity", "SecurityException during SDK init", e)
        } catch (e: Exception) {
            Log.e("MainActivity", "Compliance Init Error", e)
        }
    }

    override fun onResume() {
        super.onResume()
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
        val fineLocationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fineLocationGranted) {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }
}
