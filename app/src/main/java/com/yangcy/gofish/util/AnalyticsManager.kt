package com.yangcy.gofish.util

import android.content.Context
import com.umeng.analytics.MobclickAgent

/**
 * 友盟埋点统一管理类
 * 负责定义所有事件 ID 并提供追踪方法
 */
object AnalyticsManager {

    // --- 事件 ID 定义 ---
    
    // 首页相关
    const val EVENT_SPLASH_ENTER = "splash_enter_click" // 点击开屏页进入按钮
    const val EVENT_TAB_SWITCH = "tab_switch"           // 底部导航栏切换
    
    // 钓点地图相关
    const val EVENT_MAP_LONG_PRESS = "map_long_press"   // 地图长按选点
    const val EVENT_ADD_SPOT = "add_fishing_spot"       // 添加钓点成功
    const val EVENT_DELETE_SPOT = "delete_fishing_spot" // 删除钓点
    const val EVENT_SHARE_SPOT = "share_spot_code"      // 生成分享口令
    const val EVENT_IMPORT_SPOT = "import_spot_code"    // 导入口令成功
    const val EVENT_LAUNCH_NAVI = "launch_navi"         // 发起高德导航
    const val EVENT_MAP_SEARCH = "map_search_spot"      // 在地图搜索钓点
    
    // 鱼类图鉴相关
    const val EVENT_FISH_DETAIL = "view_fish_detail"    // 查看鱼类详情
    const val EVENT_FISH_SEARCH = "fish_catalog_search" // 搜索鱼类
    const val EVENT_FISH_FILTER = "fish_catalog_filter" // 筛选鱼类分类
    
    // 渔获日记相关
    const val EVENT_ADD_CATCH = "add_catch_log"         // 添加渔获记录
    const val EVENT_DELETE_CATCH = "delete_catch_log"   // 删除渔获记录

    /**
     * 追踪自定义事件
     */
    fun trackEvent(context: Context, eventId: String, properties: Map<String, Any>? = null) {
        if (properties == null) {
            MobclickAgent.onEvent(context, eventId)
        } else {
            MobclickAgent.onEventObject(context, eventId, properties)
        }
    }
    
    /**
     * 页面访问开始 (针对 Compose 手动管理)
     */
    fun onPageStart(pageName: String) {
        MobclickAgent.onPageStart(pageName)
    }

    /**
     * 页面访问结束
     */
    fun onPageEnd(pageName: String) {
        MobclickAgent.onPageEnd(pageName)
    }
}
