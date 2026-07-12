# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- 友盟 (Umeng) 混淆规则 ---

# 1. 基础组件 & 统计 (Common & Analytics)
-keep class com.umeng.** {*;}
-keep class com.uc.** {*;}
-keep class com.efs.** {*;}
-keep interface com.umeng.** {*;}
-keep enum com.umeng.** {*;}

-keepclassmembers class * {
   public <init> (org.json.JSONObject);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 2. 性能监控 (APM / UMCrash)
-keep class com.umeng.umcrash.** {*;}

# 3. 合规与安全 (ASMS / 设备标识)
-keep class com.zui.** {*;}
-keep class com.miui.** {*;}
-keep class com.hw.** {*;}
-keep class com.he.loader.** {*;}

# 4. 忽略警告 (防止编译报错)
-dontwarn com.umeng.**
-dontwarn com.efs.**
-dontwarn com.uc.**
