package com.yangcy.gofish

import android.Manifest
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
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yangcy.gofish.ui.screens.MainScreen
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
        
        // AMap Privacy Policy Compliance
        try {
            // Using BuildConfig to avoid hardcoding the API key in the source code
            com.amap.api.maps.MapsInitializer.setApiKey(BuildConfig.AMAP_API_KEY)
            com.amap.api.maps.MapsInitializer.updatePrivacyShow(this, true, true)
            com.amap.api.maps.MapsInitializer.updatePrivacyAgree(this, true)
            com.amap.api.location.AMapLocationClient.updatePrivacyShow(this, true, true)
            com.amap.api.location.AMapLocationClient.updatePrivacyAgree(this, true)
        } catch (e: Exception) {
            Log.e("MainActivity", "AMap Privacy Init Error", e)
        }

        enableEdgeToEdge()

        // Check and request location permissions
        checkLocationPermissions()
        
        // Only request WRITE_SETTINGS if we really don't have it and it's bothering us
        // But to avoid "looping" or "blocking", let's make it more passive
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(this)) {
            // Log it, but don't force jump every time if user keeps denying
            Log.w("MainActivity", "Missing WRITE_SETTINGS - map might be slow")
        }

        setContent {
            MyApplicationTheme {
                val viewModel: FishViewModel = viewModel()
                MainScreen(viewModel = viewModel)
            }
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
