package com.yangcy.gofish.util

import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIApplication

import platform.UIKit.UIView
import platform.UIKit.UILabel
import platform.UIKit.UIColor
import platform.UIKit.UIScreen
import platform.UIKit.NSTextAlignmentCenter
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGPointMake
import kotlinx.cinterop.useContents
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual fun showToast(message: String) {
    val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
    val rootView = rootViewController.view
    
    val toastView = UIView(frame = CGRectMake(0.0, 0.0, 260.0, 50.0)).apply {
        backgroundColor = UIColor.blackColor.colorWithAlphaComponent(0.8)
        layer.cornerRadius = 10.0
        clipsToBounds = true
        alpha = 0.0
    }
    
    val toastLabel = UILabel(frame = CGRectMake(10.0, 5.0, 240.0, 40.0)).apply {
        textColor = UIColor.whiteColor
        textAlignment = NSTextAlignmentCenter
        text = message
        numberOfLines = 2L
        font = platform.UIKit.UIFont.systemFontOfSize(14.0)
    }
    
    toastView.addSubview(toastLabel)
    
    val screenSize = UIScreen.mainScreen.bounds
    val screenWidth = screenSize.useContents { size.width }
    val screenHeight = screenSize.useContents { size.height }
    toastView.setCenter(CGPointMake(screenWidth / 2.0, screenHeight - 120.0))
    
    rootView.addSubview(toastView)
    
    UIView.animateWithDuration(
        duration = 0.3,
        animations = {
            toastView.alpha = 1.0
        },
        completion = { _ ->
            UIView.animateWithDuration(
                duration = 0.3,
                delay = 1.5,
                options = 0u,
                animations = {
                    toastView.alpha = 0.0
                },
                completion = { _ ->
                    toastView.removeFromSuperview()
                }
            )
        }
    )
}

actual val analytics: AnalyticsProvider = object : AnalyticsProvider {
    override fun trackEvent(eventId: String, properties: Map<String, Any>?) {
        println("iOS Analytics Event: $eventId, Props: $properties")
    }

    override fun onPageStart(pageName: String) {
        println("iOS Page Start: $pageName")
    }

    override fun onPageEnd(pageName: String) {
        println("iOS Page End: $pageName")
    }
}

actual fun openMapNavigation(lat: Double, lon: Double, name: String) {
    val amapUrl = platform.Foundation.NSURL.URLWithString("iosamap://navi?sourceApplication=GoFish&lat=$lat&lon=$lon&dev=0&style=2")
    if (amapUrl != null && platform.UIKit.UIApplication.sharedApplication.canOpenURL(amapUrl)) {
        platform.UIKit.UIApplication.sharedApplication.openURL(amapUrl)
    } else {
        val appleMapsUrl = platform.Foundation.NSURL.URLWithString("http://maps.apple.com/?daddr=$lat,$lon&dirflg=d")
        if (appleMapsUrl != null) {
            platform.UIKit.UIApplication.sharedApplication.openURL(appleMapsUrl)
        }
    }
}
