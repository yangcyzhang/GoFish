# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/edison/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# For Compose Multiplatform
-keepclassmembers class **.Res$* {
    public static ** *;
}

# For Room
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Entity
-keep class * extends androidx.room.Dao

# For AMap
-keep class com.amap.api.** {*;}
-keep class com.autonavi.** {*;}
-keep class com.amap.api.maps.** {*;}
-keep class com.amap.api.location.** {*;}
-keep class com.amap.api.search.** {*;}

# For Umeng
-keep class com.umeng.** {*;}
-keep class com.uc.** {*;}
-keep class com.efs.** {*;}

# For Ktor / Kotlinx Serialization
-keep class kotlinx.serialization.** {*;}
-keepattributes *Annotation*,EnclosingMethod,Signature
-keepnames class kotlinx.serialization.internal.GeneratedSerializer* {
    ** INSTANCE;
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}

# General
-dontwarn android.util.Half
-dontwarn androidx.compose.ui.platform.AndroidComposeView
-dontwarn io.ktor.**
