# GoFish - 专业级垂钓百科与实战助手 | Professional Angling Encyclopedia & Smart Assistant

[中文](#中文) | [English](#english)

---

## 中文

GoFish 是一款专为垂钓爱好者设计的 Android 应用，结合了高德地图 API、实时气象数据和地理信息，为用户提供全方位的垂钓支持。

### 核心功能

- **智能钓点地图**: 集成了高德地图 API，支持自定义水滴形钓点标记、导航、分享及私密导入口令。
- **鱼类实战图鉴**: 详细的鱼类百科，包括习性、分布、针对性钓法（路亚/手竿）及保护等级。
- **渔获记录日记**: 结构化记录每一次垂钓过程，记录位置、用饵及心得。
- **科学气象建议**: 结合实时气象数据，针对不同天气提供即时的垂钓策略建议。

### 技术栈

- **开发语言**: Kotlin
- **UI 框架**: Jetpack Compose (全现代化响应式 UI)
- **地图服务**: 高德地图 (AMap SDK) + 自定义 Canvas 绘制
- **数据存储**: Room Database (本地持久化)
- **架构**: MVVM + Coroutines/Flow

### 快速开始

#### 前置要求
- [Android Studio Ladybug+](https://developer.android.com/studio)
- Android SDK 33+

#### 运行步骤
1. 克隆或下载本项目。
2. 在 Android Studio 中打开项目文件夹。
3. 在根目录创建 `.env` 文件，并参考 `.env.example` 配置 `AMAP_API_KEY`。
4. 进行 Gradle 同步并运行应用。

---

## English

GoFish is a professional Android application designed for angling enthusiasts. By integrating AMap SDK, real-time weather analytics, and geospatial data, GoFish provides comprehensive support for every fishing trip.

### Key Features

- **Smart Fishing Map**: Customized AMap integration with teardrop markers, precision navigation, and a unique "Share-Code" system for private location exchange.
- **Tactical Fish Encyclopedia**: Comprehensive reference for various species, covering habits, habitats, specific angling strategies (Lure/Bait), and conservation status.
- **Catch Log & Diary**: A structured logging system to archive your successes, including locations, bait used, and personal reflections.
- **Scientific Weather Insights**: Leverages real-time weather data to provide instant tactical advice based on current conditions.

### Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Modern & Reactive UI)
- **Map Service**: AMap SDK + Custom Canvas Rendering
- **Persistence**: Room Database
- **Architecture**: MVVM + Coroutines/Flow

### Getting Started

#### Prerequisites
- [Android Studio Ladybug+](https://developer.android.com/studio)
- Android SDK 33+

#### Installation
1. Clone or download the repository.
2. Open the project in Android Studio.
3. Create a `.env` file in the root directory and configure `AMAP_API_KEY` based on `.env.example`.
4. Sync Gradle and run the app.

---

## 证书与许可 | License
本项目采用 MIT 许可 | This project is licensed under the MIT License.
