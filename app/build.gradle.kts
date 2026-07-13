plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.yangcy.gofish"
  compileSdk = 37

  defaultConfig {
    applicationId = "com.yangcy.gofish"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    // 2. ABI 过滤：只保留主流的手机架构，完全移除 x86 等模拟器架构减小体积
    ndk {
      abiFilters += listOf("arm64-v8a")
    }
  }

  // 1. 资源配置优化：只保留中英文资源，移除其他库自带的多语言包
  androidResources {
    localeFilters += listOf("zh", "zh-rCN", "en")
  }

  signingConfigs {
    create("release") {
      val keystorePath = "${rootDir}/baohu_release.jks"
      storeFile = file(keystorePath)
      storePassword = "baohu123"
      keyAlias = "baohu_key"
      keyPassword = "baohu123"
    }
    create("debugConfig") {
      val localDebugKeystore = file("${rootDir}/debug.keystore")
      if (localDebugKeystore.exists()) {
        storeFile = localDebugKeystore
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = true // 开启 PNG 优化
      isMinifyEnabled = true
      isShrinkResources = true // 开启资源缩减：自动删除未使用的图片、布局等
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
      
      // 开启 R8 进阶优化
      setProguardFiles(listOf(
          getDefaultProguardFile("proguard-android-optimize.txt"),
          "proguard-rules.pro"
      ))
    }
    debug {
      val localDebugKeystore = file("${rootDir}/debug.keystore")
      if (localDebugKeystore.exists()) {
        signingConfig = signingConfigs.getByName("debugConfig")
      }
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }

  lint {
    checkReleaseBuilds = false
    abortOnError = false
  }
}

secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env"
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.play.services.location)
  implementation(libs.retrofit)
  
  // Umeng Analytics
  implementation(libs.umeng.common)
  implementation(libs.umeng.asms)
  implementation(libs.umeng.apm)

  // AMap SDK
  implementation("com.amap.api:3dmap:10.0.600")
  implementation("com.amap.api:search:7.9.0") // Downgrade Search to avoid core conflict with 3DMap 10.x

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
