package com.yangcy.gofish

import android.app.Application
import com.umeng.commonsdk.UMConfigure
import com.amap.api.maps.MapsInitializer
import com.amap.api.location.AMapLocationClient

class GoFishApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 1. 设置高德地图合规 (仅展示，不代表同意)
        MapsInitializer.updatePrivacyShow(this, true, true)
        AMapLocationClient.updatePrivacyShow(this, true, true)
        
        // 2. 友盟预初始化 (合规要求：必须在 Application.onCreate 中调用)
        // preInit 不会采集任何隐私信息
        UMConfigure.preInit(this, BuildConfig.UMENG_APP_KEY, "Umeng")
    }
}
