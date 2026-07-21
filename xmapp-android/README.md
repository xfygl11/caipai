# xmapp-android - 个人助手TV / Personal Assistant TV

> 结合基于 WebView 的 JavaScript 逻辑与原生的 ExoPlayer 视频播放的混合 Android TV 应用程序。
> A hybrid Android TV application combining WebView-based JavaScript logic with native ExoPlayer video playback.

## 目录 / Table of Contents

- [项目概述 / Project Overview](#项目概述--project-overview)
- [功能特性 / Features](#功能特性--features)
- [技术栈 / Tech Stack](#技术栈--tech-stack)
- [目录结构 / Directory Structure](#目录结构--directory-structure)
- [构建与运行 / Build & Run](#构建与运行--build--run)
- [架构 / Architecture](#架构--architecture)
  - [混合架构 / Hybrid Architecture](#混合架构--hybrid-architecture)
  - [原生外壳 (Android) / Native Shell (Android)](#原生外壳-android--native-shell-android)
  - [JavaScript 引擎 (Web 资源) / JavaScript Engine (Web Assets)](#javascript-引擎-web-资源--javascript-engine-web-assets)
  - [原生-JS 桥接 / Native-JS Bridges](#原生-js-桥接--native-js-bridges)
- [JS 模块详解 / JS Module Breakdown](#js-模块详解--js-module-breakdown)
  - [核心模块 / Core Modules](#核心模块--core-modules)
  - [库模块 / Library Modules](#库模块--library-modules)
- [电影引擎 / Movie Engine](#电影引擎--movie-engine)
- [直播电视 / Live TV](#直播电视--live-tv)
- [仓库管理 / Repository Management](#仓库管理--repository-management)
- [搜索 / Search](#搜索--search)
- [缓存 / Caching](#缓存--caching)
- [ExoPlayer 集成 / ExoPlayer Integration](#exoplayer-集成--exoplayer-integration)
  - [全屏播放器 / Full-Screen Player](#全屏播放器--full-screen-player)
  - [页面内覆盖播放器 / In-Page Overlay Player](#页面内覆盖播放器--in-page-overlay-player)
- [配置 / Configuration](#配置--configuration)
- [故障排除 / Troubleshooting](#故障排除--troubleshooting)
- [API 参考 / API Reference](#api-参考--api-reference)
  - [NativeHttp API / NativeHttp API](#nativehttp-api--nativehttp-api)
  - [LocalStorageBridge API / LocalStorageBridge API](#localstoragebridge-api--localstoragebridge-api)
  - [ExoPlayer 包装器 API / ExoPlayer Wrapper API](#exoplayer-包装器-api--exo-player-wrapper-api)

---

## 项目概述 / Project Overview

**xmapp-android**（包名：`webapp.newcloud.lottery.movie`）是一款名为 **"个人助手TV"** 的混合 Android TV 应用程序。它将基于 WebView 的 JavaScript 运行时与原生 Android 组件相结合，提供丰富的媒体体验，包括：

**xmapp-android** (package: `webapp.newcloud.lottery.movie`) is a hybrid Android TV application named **"个人助手TV"** (Personal Assistant TV). It combines a WebView-based JavaScript runtime with native Android components to deliver a rich media experience including:

- 电影/电视节目浏览与播放
- 电视频道直播流媒体
- 彩票号码抽奖
- 多源并发搜索
- 可配置的内容源（爬虫/蜘蛛）
- 本地缓存与持久化

- Movie/TV show browsing and playback
- Live TV channel streaming
- Lottery number drawing
- Multi-source concurrent search
- Configurable content sources (spiders/crawlers)
- Local caching and persistence

该应用采用**混合架构**：Android 外壳提供 WebView 容器、原生视频播放（ExoPlayer）和平台桥接（HTTP、存储），而所有业务逻辑（UI 渲染、数据获取、播放器控制）均在 JavaScript 中运行。

The app uses a **hybrid architecture**: the Android shell provides the WebView container, native video playback (ExoPlayer), and platform bridges (HTTP, storage), while all business logic (UI rendering, data fetching, player control) runs in JavaScript.

---

## 功能特性 / Features

| 功能 / Feature | 描述 / Description |
|---------|-------------|
| 电影引擎 / Movie Engine | 动态内容获取、缓存、渲染及从可配置源播放 |
| 直播电视 / Live TV | 频道分组、直播流播放、通过 `live2cms` 支持 EPG |
| 仓库管理 / Repository Management | 添加/移除/配置内容源仓库（爬虫） |
| 搜索 / Search | 多源并发搜索与结果聚合 |
| 缓存 / Caching | 电影数据、搜索结果和源配置的本地缓存 |
| 视频播放 / Video Playback | 全屏 ExoPlayer 和带手势控制的页面内覆盖播放器 |
| 站点配置 / Site Configuration | 动态站点映射、API 端点和爬虫加载 |
| 持久化 / Persistence | 通过桥接将 LocalStorage 映射到 Android SharedPreferences |
| 彩票 / Lottery | 内置彩票号码抽奖功能 |
| UI 框架 / UI Framework | 基于自定义 JS 的响应式 TV 友好布局 |

---

## 技术栈 / Tech Stack

### Android (原生 / Native)

| 组件 / Component | 版本/详情 / Version/Details |
|-----------|-----------------|
| 最低 SDK / Min SDK | 21 (Android 5.0) |
| 目标 SDK / Target SDK | 34 (Android 14) |
| 编译 SDK / Compile SDK | 34 |
| Gradle 插件 / Gradle Plugin | com.android.application 8.6.1 |
| Kotlin | 2.0.20 |
| ExoPlayer | 2.19.1 |
| AppCompat | 1.6.1 |
| Material | 1.11.0 |
| ConstraintLayout | 2.1.4 |
| WebView | Android System WebView（内置 / Bundled） |

### JavaScript (Web 资源 / Web Assets)

| 模块 / Module | 用途 / Purpose |
|--------|---------|
| `app.js` | 核心入口、初始化、路由 |
| `nc-movie-engine.js` | 电影数据获取、缓存、渲染 |
| `nc-live.js` | 直播电视频道管理与播放 |
| `nc-repo.js` | 仓库/源管理 |
| `nc-search.js` | 多源并发搜索 |
| `nc-site-manage.js` | 站点配置与 API 映射 |
| `nc-db.js` | LocalStorage 的原生数据库包装器 |
| `nc-ui.js` | UI 辅助函数（渲染、DOM 操作） |
| `nc-page.js` | 页面路由与回退处理 |
| `nc-player.js` | 视频播放器手势控制 |
| `nc-cache.js` | 缓存策略与管理 |
| `exo-player-wrapper.js` | 原生 ExoPlayer 的 JavaScript 接口 |
| `00-shim.js` | WebView 填充（localStorage、navigator 等） |

### 库模块 / Library Modules

| 文件 / File | 用途 / Purpose |
|-----------|---------|
| `lib/live2cms.js` | 直播电视到 CMS 转换器 |
| `lib/模板.js` | 爬虫/模板基类 |
| `lib/mod.js` | 模块加载器和依赖管理器 |
| `lib/spider.js` | 内容爬取的基础爬虫类 |
| `lib/http.js` | HTTP 客户端包装器 |
| `lib/net.js` | 网络实用函数 |

---

## 目录结构 / Directory Structure

```
xmapp-android/
├── app/
│   ├── build.gradle.kts              # 应用级构建配置 / App-level build configuration
│   └── src/main/
│       ├── AndroidManifest.xml        # Android 清单（权限、活动 / Permissions, Activities）
│       ├── java/webapp/newcloud/lottery/movie/
│       │   ├── WebAppActivity.java    # 主 WebView 活动 / Main WebView activity
│       │   ├── NativeHttp.java        # HTTP 桥接（JS -> Android）
│       │   ├── LocalStorageBridge.java # 存储桥接（JS -> SharedPreferences）
│       │   └── player/
│       │       ├── ExoPlayerActivity.java          # 全屏原生播放器 / Full-screen native player
│       │       ├── InPagePlayerOverlay.java        # 页面内覆盖播放器 / In-page overlay player
│       │       └── InlinePlayerOverlay.java        # 替代覆盖播放器 / Alternative overlay player
│       ├── res/
│       │   ├── layout/
│       │   │   ├── activity_webapp.xml    # 主活动布局 / Main activity layout
│       │   │   ├── activity_exoplayer.xml # 播放器活动布局 / Player activity layout
│       │   │   └── activity_inline_player_overlay.xml  # 覆盖布局 / Overlay layout
│       │   ├── values/
│       │   │   ├── themes.xml         # 应用主题（DayNight）
│       │   │   ├── colors.xml         # 配色方案 / Color palette
│       │   │   └── strings.xml        # 字符串资源 / String resources
│       │   ├── drawable/              # 图标和可绘制资源 / Icons and drawables
│       │   └── mipmap-*/              # 启动器图标 / Launcher icons
│       └── assets/
│           ├── index.html             # 主 HTML 外壳 / Main HTML shell
│           ├── css/
│           │   ├── style.css          # 全局样式 / Global styles
│           │   └── nc-ux.css          # 自定义 UX 样式 / Custom UX styles
│           └── js/
│               ├── 00-shim.js         # WebView 填充 / WebView polyfills
│               ├── app.js             # 核心入口 / Core entry point
│               ├── nc-movie-engine.js # 电影引擎 / Movie engine
│               ├── nc-live.js         # 直播电视 / Live TV
│               ├── nc-repo.js         # 仓库管理 / Repository management
│               ├── nc-search.js       # 搜索 / Search
│               ├── nc-site-manage.js  # 站点配置 / Site configuration
│               ├── nc-db.js           # 数据库包装器 / DB wrapper
│               ├── nc-ui.js           # UI 辅助 / UI helpers
│               ├── nc-page.js         # 页面路由 / Page routing
│               ├── nc-player.js       # 播放器手势 / Player gestures
│               ├── nc-cache.js        # 缓存 / Caching
│               ├── exo-player-wrapper.js # ExoPlayer JS 接口 / ExoPlayer JS interface
│               └── lib/
│                   ├── live2cms.js    # Live2CMS 转换器 / Live2CMS converter
│                   ├── 模板.js         # 爬虫模板 / Spider templates
│                   ├── mod.js         # 模块加载器 / Module loader
│                   ├── spider.js      # 基础爬虫 / Base spider
│                   ├── http.js        # HTTP 客户端 / HTTP client
│                   └── net.js         # 网络工具 / Network utils
├── build.gradle.kts                   # 根级构建配置 / Root build configuration
├── settings.gradle.kts                # 项目设置 / Project settings
├── gradle.properties                  # Gradle 属性 / Gradle properties
└── README.md                          # 本文件 / This file
```

---

## 构建与运行 / Build & Run

### 前置要求 / Prerequisites

- Android Studio Hedgehog (2023.1.1) 或更高版本 / or later
- JDK 17 或更高版本 / or later
- Android SDK 34（目标 / target）和 SDK 21（最低 / min）
- Gradle 8.6+

### 构建命令 / Build Commands

```bash
# 清理构建 / Clean build
./gradlew clean

# 调试 APK / Debug APK
./gradlew assembleDebug

# 发布 APK（需要签名配置 / requires signing config）
./gradlew assembleRelease

# 安装调试 APK / Install debug APK
./gradlew installDebug
```

### 在设备/模拟器上运行 / Run on Device/Emulator

1. 连接 Android TV 设备或启动 Android TV 模拟器。
   Connect an Android TV device or launch an Android TV emulator.

2. 在 Android Studio 中，选择已连接的设备并单击 **运行**。
   In Android Studio, select the connected device and click **Run**.

3. 或者通过 ADB 安装：
   Alternatively, install via ADB:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

### Gradle 属性 / Gradle Properties

`gradle.properties` 中的关键设置 / Key settings:

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

---

## 架构 / Architecture

### 混合架构 / Hybrid Architecture

```
┌─────────────────────────────────────────────────────┐
│                   Android 原生层 / Native Layer       │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────┐ │
│  │ WebAppActivity│  │ ExoPlayer    │  │ 原生      │ │
│  │ (WebView)    │  │ Activity     │  │ 桥接      │ │
│  └──────┬───────┘  └──────┬───────┘  └─────┬─────┘ │
│         │                 │                │        │
│         └────────┬────────┴────────────────┘        │
│                  ▼                                  │
│  ┌──────────────────────────────────────────────┐  │
│  │           JavaScript 运行时 (WebView)          │  │
│  │  ┌─────────┐ ┌─────────┐ ┌────────────────┐ │  │
│  │  │ 电影    │ │ 直播电视│ │ 搜索/仓库     │ │  │
│  │  │ 引擎    │ │ 引擎    │ │ 引擎           │ │  │
│  │  └─────────┘ └─────────┘ └────────────────┘ │  │
│  └──────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

该应用由两个主要层组成 / The app consists of two main layers:

1. **原生外壳 (Android)**：提供 WebView 容器、原生视频播放、平台桥接和应用生命周期管理。
   **Native Shell (Android)**: Provides the WebView container, native video playback, platform bridges, and app lifecycle management.

2. **JavaScript 引擎 (Web 资源)**：包含所有业务逻辑、UI 渲染、数据获取和播放器控制。
   **JavaScript Engine (Web Assets)**: Contains all business logic, UI rendering, data fetching, and player control.

### 原生外壳 (Android) / Native Shell (Android)

#### WebAppActivity (`WebAppActivity.java`)

- **角色 / Role**：承载 WebView 并加载 `index.html` 的主活动。
  Main activity that hosts the WebView and loads `index.html`.
- **关键职责 / Key Responsibilities**：
  - 使用自定义设置初始化 WebView（启用 JavaScript、DOM 存储、混合内容支持）。
    Initialize WebView with custom settings (JavaScript enabled, DOM storage, mixed content support).
  - 注册 JavaScript 接口（`LocalStorageBridge`、`NativeHttp`、`InPagePlayerOverlay`）。
    Register JavaScript interfaces (`LocalStorageBridge`, `NativeHttp`, `InPagePlayerOverlay`).
  - 处理导航（返回键 presses、URL 加载）。
    Handle navigation (back press, URL loading).
  - 管理 WebView 生命周期（恢复、暂停、销毁）。
    Manage WebView lifecycle (resume, pause, destroy).
  - 从 `file:///android_asset/index.html` 加载本地资源。
    Load local assets from `file:///android_asset/index.html`.

#### WebView 设置 / WebView Settings

```java
WebSettings settings = webView.getSettings();
settings.setJavaScriptEnabled(true);
settings.setDomStorageEnabled(true);
settings.setAllowFileAccess(true);
settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
settings.setMediaPlaybackRequiresUserGesture(false);
```

#### AndroidManifest.xml

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- 内容获取的互联网访问 / Internet access for content fetching -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />

    <!-- 缓存的存储 / Storage for caching -->
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.XmappAndroid.DayNight">

        <!-- 主 WebView 活动 / Main WebView Activity -->
        <activity
            android:name=".WebAppActivity"
            android:exported="true"
            android:screenOrientation="landscape">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
                <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- 全屏 ExoPlayer 活动 / Full-screen ExoPlayer Activity -->
        <activity
            android:name=".player.ExoPlayerActivity"
            android:exported="false"
            android:screenOrientation="landscape"
            android:configChanges="orientation|screenSize|keyboardHidden"
            android:theme="@style/Theme.XmappAndroid.FullScreen" />

    </application>
</manifest>
```

### JavaScript 引擎 (Web 资源) / JavaScript Engine (Web Assets)

所有 JavaScript 模块均从 `assets/js/` 目录加载。入口点是 `app.js`，它初始化运行时、加载依赖项并启动应用程序。

All JavaScript modules are loaded from the `assets/js/` directory. The entry point is `app.js`, which initializes the runtime, loads dependencies, and starts the application.

#### 初始化流程 / Initialization Flow

1. `00-shim.js` 首先运行，填充 `localStorage`、`navigator` 和其他缺少的 WebView API。
   `00-shim.js` runs first, polyfilling `localStorage`, `navigator`, and other missing WebView APIs.

2. `app.js` 初始化核心运行时，注册事件侦听器，并路由到适当的页面。
   `app.js` initializes the core runtime, registers event listeners, and routes to the appropriate page.

3. 动态模块（电影引擎、直播电视、搜索）按需加载。
   Dynamic modules (movie engine, live TV, search) are loaded on demand.

### 原生-JS 桥接 / Native-JS Bridges

JavaScript 运行时通过三个主要桥接与 Android 原生代码通信：

The JavaScript runtime communicates with Android native code through three primary bridges:

| 桥接 / Bridge | Java 类 / Java Class | JS 接口 / JS Interface | 用途 / Purpose |
|--------|-----------|--------------|---------|
| HTTP 桥接 / HTTP Bridge | `NativeHttp` | `window.NativeHttp` | 从 JS 通过 Android 网络线程发起 HTTP GET/POST 请求 |
| 存储桥接 / Storage Bridge | `LocalStorageBridge` | `window.LocalStorageBridge` | 将 JS `localStorage` 同步到 Android `SharedPreferences` |
| 播放器桥接 / Player Bridge | `InPagePlayerOverlay` | `window.InPagePlayerOverlay` | 从 JS 控制页面内 ExoPlayer 覆盖层 |

---

## JS 模块详解 / JS Module Breakdown

### 核心模块 / Core Modules

#### `app.js` - 核心入口 / Core Entry Point

- 初始化应用程序运行时。
  Initializes the application runtime.
- 按顺序加载和执行依赖模块。
  Loads and executes dependent modules in order.
- 设置全局事件处理器（键盘导航、电视遥控器焦点管理）。
  Sets up global event handlers (keyboard navigation, focus management for TV remote).
- 根据 URL 参数或默认主页路由到适当页面。
  Routes to the appropriate page based on URL parameters or default home screen.
- 管理应用程序生命周期（初始化、恢复、暂停）。
  Manages the application lifecycle (init, resume, pause).

```javascript
// app.js - 关键初始化序列 / Key initialization sequence
(function() {
    'use strict';
    
    // 首先加载填充 / Load shim first
    loadScript('js/00-shim.js');
    
    // 加载库 / Load libraries
    loadScript('js/lib/mod.js');
    loadScript('js/lib/spider.js');
    loadScript('js/lib/http.js');
    loadScript('js/lib/net.js');
    loadScript('js/lib/live2cms.js');
    
    // 加载核心模块 / Load core modules
    loadScript('js/nc-db.js');
    loadScript('js/nc-cache.js');
    loadScript('js/nc-ui.js');
    loadScript('js/nc-page.js');
    loadScript('js/nc-site-manage.js');
    loadScript('js/nc-repo.js');
    loadScript('js/nc-search.js');
    loadScript('js/nc-movie-engine.js');
    loadScript('js/nc-live.js');
    loadScript('js/nc-player.js');
    loadScript('js/exo-player-wrapper.js');
    
    // 初始化应用程序 / Initialize application
    App.init();
})();
```

#### `nc-movie-engine.js` - 电影引擎 / Movie Engine

- **用途 / Purpose**：核心电影/电视节目数据获取、缓存和渲染。
  Core movie/TV show data fetching, caching, and rendering.
- **关键函数 / Key Functions**：
  - `fetchMovieList(category, page)`：按分类和页码获取电影列表。
  - `fetchMovieDetail(tid)`：获取特定电影的详细信息。
  - `playMovie(tid, playUrl)`：通过 ExoPlayer 启动播放。
  - `renderMovieGrid(movies)`：渲染电影网格 UI。
  - `cacheMovieData(key, data, ttl)`：带 TTL 缓存电影数据。
- **数据流程 / Data Flow**：
  1. 用户选择分类。
     User selects a category.
  2. 引擎检查本地缓存中是否有现有数据。
     Engine checks local cache for existing data.
  3. 如果已缓存且未过期，从缓存渲染。
     If cached and not expired, render from cache.
  4. 如果未缓存，通过 `NativeHttp` 从配置源获取。
     If not cached, fetch from configured source via `NativeHttp`.
  5. 解析响应，提取电影列表，缓存结果。
     Parse response, extract movie list, cache result.
  6. 渲染电影网格 UI。
     Render movie grid UI.

#### `nc-live.js` - 直播电视 / Live TV

- **用途 / Purpose**：直播电视频道管理、分组和播放。
  Live TV channel management, grouping, and playback.
- **关键函数 / Key Functions**：
  - `loadLiveChannels()`：从配置源获取频道列表。
  - `getChannelGroups()`：返回按分类分组的频道列表。
  - `playChannel(channelId)`：启动直播流播放。
  - `renderChannelList(groups)`：渲染频道列表 UI。
- **频道分组 / Channel Groups**：频道按类别分类（如 CCTV、卫视、地方），并以分组侧边栏显示。
  Channels are categorized (e.g., CCTV, Satellite, Local) and displayed in a grouped sidebar.

#### `nc-search.js` - 搜索 / Search

- **用途 / Purpose**：多源并发搜索与结果聚合。
  Multi-source concurrent search with result aggregation.
- **关键函数 / Key Functions**：
  - `search(keyword, sources)`：跨多个源并发搜索。
  - `aggregateResults(results)`：合并和去重搜索结果。
  - `renderSearchResults(results)`：渲染搜索结果 UI。
- **并发性 / Concurrency**：对多个源使用并行 HTTP 请求，在所有请求完成或超时后聚合结果。
  Uses parallel HTTP requests to multiple sources, aggregates results when all complete or timeout.

#### `nc-site-manage.js` - 站点配置 / Site Configuration

- **用途 / Purpose**：管理内容源站点、API 端点和爬虫配置。
  Manage content source sites, API endpoints, and spider configurations.
- **关键函数 / Key Functions**：
  - `loadSiteConfig(siteId)`：加载特定站点的配置。
  - `saveSiteConfig(siteId, config)`：保存站点配置。
  - `getAllSites()`：返回所有已配置站点的列表。
  - `mapApiEndpoint(siteId, apiType)`：将 API 类型映射到实际的端点 URL。

#### `nc-repo.js` - 仓库管理 / Repository Management

- **用途 / Purpose**：管理内容源仓库（爬虫/采集器）。
  Manage content source repositories (spiders/crawlers).
- **关键函数 / Key Functions**：
  - `addRepo(repoUrl)`：添加新仓库。
  - `removeRepo(repoId)`：移除仓库。
  - `syncRepos()`：从远程配置同步仓库列表。
  - `getRepoById(repoId)`：获取仓库详情。

#### `nc-db.js` - 数据库包装器 / Database Wrapper

- **用途 / Purpose**：`localStorage` 之上用于结构化数据操作的抽象层。
  Abstraction layer over `localStorage` for structured data operations.
- **关键函数 / Key Functions**：
  - `db.get(key)`：按键获取值。
  - `db.set(key, value)`：按键设置值。
  - `db.remove(key)`：移除键。
  - `db.clear()`：清除所有数据。
  - `db.keys()`：获取所有键。

#### `nc-ui.js` - UI 辅助 / UI Helpers

- **用途 / Purpose**：可复用的 UI 渲染函数。
  Reusable UI rendering functions.
- **关键函数 / Key Functions**：
  - `ui.renderGrid(items, template)`：渲染项目网格。
  - `ui.renderList(items, template)`：渲染项目列表。
  - `ui.showLoading()`：显示加载动画。
  - `ui.hideLoading()`：隐藏加载动画。
  - `ui.showToast(message)`：显示提示通知。

#### `nc-page.js` - 页面路由 / Page Routing

- **用途 / Purpose**：处理页面导航和回退。
  Handle page navigation and fallback.
- **关键函数 / Key Functions**：
  - `page.navigate(route, params)`：导航到路由。
  - `page.getRoute()`：获取当前路由。
  - `page.registerRoute(route, handler)`：注册路由处理器。
  - `page.fallback()`：处理未知路由。

#### `nc-player.js` - 播放器手势 / Player Gestures

- **用途 / Purpose**：视频播放的手势控制（滑动调节音量、亮度、进度）。
  Gesture controls for video playback (swipe to adjust volume, brightness, progress).
- **关键函数 / Key Functions**：
  - `player.initGestures(videoElement)`：初始化手势检测。
  - `player.onSwipe(direction, amount)`：处理滑动手势。
  - `player.adjustVolume(amount)`：调节系统音量。
  - `player.adjustBrightness(amount)`：调节屏幕亮度。

#### `nc-cache.js` - 缓存 / Caching

- **用途 / Purpose**：电影数据、搜索结果和配置的本地缓存策略。
  Local caching strategy for movie data, search results, and configurations.
- **关键函数 / Key Functions**：
  - `cache.set(key, data, ttl)`：带生存时间的数据存储。
  - `cache.get(key)`：检索未过期的数据。
  - `cache.remove(key)`：移除缓存数据。
  - `cache.clearExpired()`：移除所有过期条目。
  - `cache.size()`：获取缓存条目数量。

#### `exo-player-wrapper.js` - ExoPlayer JS 接口 / ExoPlayer JS Interface

- **用途 / Purpose**：原生 ExoPlayer 控制的 JavaScript 包装器。
  JavaScript wrapper for native ExoPlayer control.
- **关键函数 / Key Functions**：
  - `exoPlayer.play(url)`：开始播放。
  - `exoPlayer.pause()`：暂停播放。
  - `exoPlayer.resume()`：恢复播放。
  - `exoPlayer.stop()`：停止并释放播放器。
  - `exoPlayer.seek(position)`：跳转到指定位置。
  - `exoPlayer.setOnCompleteListener(callback)`：设置完成回调。
  - `exoPlayer.setOnErrorListener(callback)`：设置错误回调。

### 库模块 / Library Modules

#### `lib/mod.js` - 模块加载器 / Module Loader

- JavaScript 模块的动态脚本加载器。
  Dynamic script loader for JavaScript modules.
- 处理依赖解析和加载顺序。
  Handles dependency resolution and loading order.
- 提供类似 `require()` 的功能。
  Provides `require()`-like functionality.

```javascript
// mod.js - 关键函数 / Key functions
function loadScript(src) {
    var script = document.createElement('script');
    script.src = src;
    script.async = false;
    document.head.appendChild(script);
}

function require(moduleName) {
    // 解析并返回模块 / Resolve and return module
}
```

#### `lib/spider.js` - 基础爬虫 / Base Spider

- 内容爬取爬虫的基础类。
  Base class for content crawling spiders.
- 定义获取和解析内容的通用接口。
  Defines common interface for fetching and parsing content.
- 由特定的爬虫实现继承。
  Subclassed by specific spider implementations.

```javascript
// spider.js - 基础类 / Base class
class Spider {
    constructor(config) {
        this.config = config;
        this.baseUrl = config.baseUrl || '';
    }
    
    async fetch(url, method, data) {
        // 使用 NativeHttp 桥接 / Use NativeHttp bridge
        return NativeHttp.request(url, method, data);
    }
    
    async parseList(html) {
        // 从 HTML 解析电影列表 / Parse movie list from HTML
    }
    
    async parseDetail(html) {
        // 从 HTML 解析电影详情 / Parse movie detail from HTML
    }
}
```

#### `lib/http.js` - HTTP 客户端 / HTTP Client

- `NativeHttp` 桥接的包装器。
  Wrapper around `NativeHttp` bridge.
- 提供 GET/POST 请求的便捷方法。
  Provides convenient methods for GET/POST requests.
- 处理响应解析（JSON、HTML、文本）。
  Handles response parsing (JSON, HTML, text).

```javascript
// http.js - 关键函数 / Key functions
async function httpGet(url, headers) {
    return NativeHttp.get(url, headers);
}

async function httpPost(url, data, headers) {
    return NativeHttp.post(url, data, headers);
}

async function httpJson(url) {
    var response = await httpGet(url);
    return JSON.parse(response);
}
```

#### `lib/net.js` - 网络工具 / Network Utilities

- 网络连接性检查。
  Network connectivity check.
- URL 构造和编码。
  URL construction and encoding.
- 失败请求的重试逻辑。
  Retry logic for failed requests.

#### `lib/live2cms.js` - 直播转 CMS 转换器 / Live to CMS Converter

- 将直播电视频道列表转换为 CMS 兼容格式。
  Converts live TV channel lists to CMS-compatible format.
- 使直播频道能够与电影内容一起浏览。
  Enables live channels to be browsed alongside movie content.
- 将频道元数据映射到标准电影项目结构。
  Maps channel metadata to standard movie item structure.

---

## 电影引擎 / Movie Engine

电影引擎（`nc-movie-engine.js`）是核心内容交付系统。它支持：

The movie engine (`nc-movie-engine.js`) is the core content delivery system. It supports:

### 数据源 / Data Sources

- **可配置站点 / Configurable Sites**：站点在 JSON 配置文件中定义，指定 API 端点、爬虫类型和解析规则。
  Sites are defined in JSON configuration files, specifying API endpoints, spider types, and parsing rules.

- **动态爬虫加载 / Dynamic Spider Loading**：爬虫根据站点配置动态加载。
  Spiders are loaded dynamically based on site configuration.

- **支持的格式 / Supported Formats**：JSON API、HTML 解析（通过爬虫）和 live2cms 转换。
  JSON API, HTML parsing (via spiders), and live2cms conversion.

### 获取流程 / Fetching Flow

1. **分类选择 / Category Selection**：用户选择分类（如"电影"、"电视剧"、"动漫"）。
   User selects a category (e.g., "Movies", "TV Series", "Anime").

2. **缓存检查 / Cache Check**：引擎通过 `nc-cache.js` 检查缓存中的数据。
   Engine checks `nc-cache.js` for cached data.

3. **远程获取 / Remote Fetch**：如果未缓存，通过 `NativeHttp` 从站点 API 获取。
   If not cached, fetch from site API via `NativeHttp`.

4. **解析 / Parsing**：响应由站点特定的爬虫或解析器解析。
   Response is parsed by the site-specific spider or parser.

5. **缓存 / Caching**：解析后的数据带 TTL 缓存。
   Parsed data is cached with TTL.

6. **渲染 / Rendering**：通过 `nc-ui.js` 渲染电影网格。
   Movie grid is rendered via `nc-ui.js`.

### 分页 / Pagination

- 支持无限滚动和逐页导航。
  Supports infinite scroll and page-by-page navigation.
- 每站页面大小可配置。
  Page size is configurable per site.
- 获取期间显示加载状态。
  Loading state is shown during fetch.

### 播放 / Playback

- 电影播放通过 `exo-player-wrapper.js` 启动。
  Movie playback is initiated via `exo-player-wrapper.js`.
- 支持全屏（`ExoPlayerActivity`）和页面内（`InPagePlayerOverlay`）模式。
  Supports both full-screen (`ExoPlayerActivity`) and in-page (`InPagePlayerOverlay`) modes.
- 播放历史记录在本地存储中追踪。
  Play history is tracked in local storage.

---

## 直播电视 / Live TV

直播电视模块（`nc-live.js`）提供：

The live TV module (`nc-live.js`) provides:

### 频道管理 / Channel Management

- **频道分组 / Channel Groups**：频道组织成组（CCTV、卫视、地方等）。
  Channels are organized into groups (CCTV, Satellite, Local, etc.).

- **频道列表 / Channel List**：从配置源（JSON 或爬虫）获取频道列表。
  Fetches channel list from configured source (JSON or spider).

- **EPG 支持 / EPG Support**：可以获取和显示电子节目单数据。
  Electronic Program Guide data can be fetched and displayed.

### 播放 / Playback

- 直播流通过 ExoPlayer 播放。
  Live streams are played via ExoPlayer.
- 支持 HLS (.m3u8)、MP4 和其他常见格式。
  Supports HLS (.m3u8), MP4, and other common formats.
- 频道切换即时完成（预加载下一频道）。
  Channel switching is instant (preloads next channel).

### Live2CMS 集成 / Live2CMS Integration

- 直播频道可通过 `lib/live2cms.js` 转换为 CMS 格式。
  Live channels can be converted to CMS format via `lib/live2cms.js`.
- 使直播频道能够以电影风格的浏览界面出现。
  Enables live channels to appear in movie-style browsing interfaces.

---

## 仓库管理 / Repository Management

仓库模块（`nc-repo.js`）处理：

The repository module (`nc-repo.js`) handles:

### 仓库配置 / Repository Configuration

- **添加仓库 / Add Repository**：用户可以通过提供仓库配置文件 URL 添加新仓库。
  Users can add new repositories by providing a URL to the repository config file.

- **移除仓库 / Remove Repository**：可以移除现有仓库。
  Existing repositories can be removed.

- **同步仓库 / Sync Repositories**：可以同步远程配置以更新仓库列表。
  Remote config can be synced to update repository list.

### 仓库配置格式 / Repository Config Format

```json
{
    "repos": [
        {
            "id": "repo_001",
            "name": "影视采集",
            "url": "http://example.com/spider.json",
            "api": "fishcms",
            "active": true
        }
    ]
}
```

### 爬虫加载 / Spider Loading

- 爬虫从仓库 URL 下载。
  Spiders are downloaded from repository URLs.
- 本地缓存以供离线使用。
  Cached locally for offline use.
- 按需或应用启动时更新。
  Updated on demand or on app startup.

---

## 搜索 / Search

搜索模块（`nc-search.js`）提供：

The search module (`nc-search.js`) provides:

### 多源搜索 / Multi-Source Search

- **并发请求 / Concurrent Requests**：同时搜索所有已配置站点。
  Searches across all configured sites simultaneously.

- **聚合 / Aggregation**：合并和去重所有源的搜索结果。
  Results from all sources are merged and deduplicated.

- **超时 / Timeout**：单个源搜索在可配置时长后超时（默认：5 秒）。
  Individual source searches timeout after a configurable duration (default: 5 seconds).

### 搜索流程 / Search Flow

1. 用户输入搜索关键词。
   User enters search keyword.

2. 搜索同时分发到所有活跃站点。
   Search is dispatched to all active sites concurrently.

3. 结果异步到达并聚合。
   Results arrive asynchronously and are aggregated.

4. 最终结果集以统一的网格渲染。
   Final result set is rendered in a unified grid.

### 搜索配置 / Search Configuration

- 可搜索站点按仓库配置。
  Searchable sites are configured per repository.
- 搜索 API 端点在站点配置中映射。
  Search API endpoints are mapped in site configuration.

---

## 缓存 / Caching

缓存模块（`nc-cache.js`）实现：

The caching module (`nc-cache.js`) implements:

### 缓存策略 / Cache Strategies

- **基于 TTL / TTL-Based**：每个缓存条目都有生存时间（电影数据默认：24 小时，搜索结果默认：1 小时）。
  Each cached entry has a time-to-live (default: 24 hours for movie data, 1 hour for search results).

- **惰性过期 / Lazy Expiration**：在访问时检查条目的过期情况；过期条目将被移除。
  Entries are checked for expiration on access; expired entries are removed.

- **手动清除 / Manual Clear**：用户可以清除所有缓存数据。
  Users can clear all cached data.

### 缓存数据类型 / Cached Data Types

| 类型 / Type | 默认 TTL / Default TTL | 用途 / Purpose |
|-----------|-------------|---------|
| 电影列表 / Movie Lists | 24 小时 | 分类电影列表 |
| 电影详情 / Movie Details | 7 天 | 单个电影/剧集信息 |
| 搜索结果 / Search Results | 1 小时 | 搜索查询结果 |
| 站点配置 / Site Config | 1 小时 | 站点配置数据 |
| 频道列表 / Channel List | 24 小时 | 直播电视频道数据 |

### 缓存存储 / Cache Storage

- 通过 `LocalStorageBridge` 存储在 `localStorage` 中。
  Stored in `localStorage` via `LocalStorageBridge`.
- 序列化为 JSON。
  Serialized as JSON.
- 键遵循模式：`cache:{type}:{hash}`。
  Keys follow pattern: `cache:{type}:{hash}`.

---

## ExoPlayer 集成 / ExoPlayer Integration

### 全屏播放器 / Full-Screen Player

`ExoPlayerActivity` 提供全屏原生视频播放体验。

`ExoPlayerActivity` provides a full-screen native video playback experience.

#### 从 JS 启动 / Launch from JS

```javascript
// 从 JavaScript 启动全屏播放器
// From JavaScript, launch full-screen player
exoPlayer.play('http://example.com/video.mp4');
```

#### Java 实现 / Java Implementation

```java
// ExoPlayerActivity.java - 关键设置 / Key setup
public class ExoPlayerActivity extends AppCompatActivity {
    private ExoPlayer exoPlayer;
    private SimpleExoPlayerView playerView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exoplayer);
        
        playerView = findViewById(R.id.player_view);
        exoPlayer = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(exoPlayer);
        
        String videoUrl = getIntent().getStringExtra("video_url");
        MediaItem mediaItem = MediaItem.fromUri(videoUrl);
        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.prepare();
        exoPlayer.play();
    }
    
    @Override
    protected void onDestroy() {
        exoPlayer.release();
        super.onDestroy();
    }
}
```

#### 控制 / Controls

- 播放/暂停 / Play/Pause
- 快进/快退 / Seek (forward/backward)
- 音量调节 / Volume adjustment
- 全屏切换 / Full-screen toggle
- 播放速度调节 / Playback speed adjustment

### 页面内覆盖播放器 / In-Page Overlay Player

`InPagePlayerOverlay` 提供 WebView 内的覆盖视频播放器，允许用户在观看时浏览内容。

`InPagePlayerOverlay` provides an overlay video player within the WebView, allowing users to browse content while watching.

#### 从 JS 启动 / Launch from JS

```javascript
// 从 JavaScript 启动页面内播放器
// From JavaScript, launch in-page player
InPagePlayerOverlay.play('http://example.com/video.mp4');
```

#### Java 实现 / Java Implementation

```java
// InPagePlayerOverlay.java - 关键设置 / Key setup
public class InPagePlayerOverlay extends FrameLayout {
    private ExoPlayer exoPlayer;
    private SimpleExoPlayerView playerView;
    private WebView webView;
    
    public InPagePlayerOverlay(Context context, WebView webView) {
        super(context);
        this.webView = webView;
        initPlayer();
    }
    
    private void initPlayer() {
        playerView = new SimpleExoPlayerView(getContext());
        exoPlayer = new ExoPlayer.Builder(getContext()).build();
        playerView.setPlayer(exoPlayer);
        addView(playerView);
        
        // 在 WebView 顶部定位覆盖层
        // Position overlay on top of WebView
        LayoutParams params = new LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        );
        params.gravity = Gravity.TOP | Gravity.END;
        playerView.setLayoutParams(params);
    }
    
    public void play(String url) {
        MediaItem mediaItem = MediaItem.fromUri(url);
        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.prepare();
        exoPlayer.play();
        setVisible(true);
    }
    
    public void pause() {
        exoPlayer.pause();
    }
    
    public void stop() {
        exoPlayer.stop();
        exoPlayer.clearMediaItems();
        setVisible(false);
    }
}
```

#### 功能 / Features

- 可拖拽位置 / Draggable position
- 可调整大小 / Resizeable size
- 透明度控制 / Transparency control
- 播放结束后自动隐藏 / Auto-hide after playback ends
- 手势控制（通过 `nc-player.js`）/ Gesture controls (via `nc-player.js`)

---

## 配置 / Configuration

### Android 清单权限 / Android Manifest Permissions

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

### WebView 设置 / WebView Settings

```java
WebSettings settings = webView.getSettings();
settings.setJavaScriptEnabled(true);
settings.setDomStorageEnabled(true);
settings.setAllowFileAccess(true);
settings.setAllowContentAccess(true);
settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
settings.setMediaPlaybackRequiresUserGesture(false);
settings.setCacheMode(WebSettings.LOAD_DEFAULT);
settings.setLoadWithOverviewMode(true);
settings.setUseWideViewPort(true);
```

### 站点配置 API / Site Configuration API

站点通过远程 JSON API 配置。示例配置：
Sites are configured via remote JSON APIs. Example configuration:

```json
{
    "sites": [
        {
            "id": "site_ffzy",
            "name": "非凡影视",
            "api": "fishcms",
            "ext": "",
            "url": "http://ffzy1.com/api.php/provide/vod/",
            "playUrl": "",
            "search": 1,
            "categories": ["电影", "电视剧", "动漫", "综艺"]
        }
    ]
}
```

### 仓库配置 / Repository Configuration

仓库通过远程 JSON 配置：
Repositories are configured via remote JSON:

```json
{
    "version": "1.0.0",
    "repos": [
        {
            "id": "repo_001",
            "name": "影视采集",
            "url": "http://example.com/spider.json",
            "api": "fishcms",
            "type": 3,
            "active": true
        }
    ]
}
```

---

## 故障排除 / Troubleshooting

### WebView 未加载 / WebView Not Loading

- **检查 / Check**：确保 `assets/index.html` 存在且有效。
  Ensure `assets/index.html` exists and is valid.

- **检查 / Check**：验证 WebView 设置中已启用 JavaScript。
  Verify JavaScript is enabled in WebView settings.

- **检查 / Check**：在 Android 日志中检查 WebView 错误（`adb logcat | grep Chromium`）。
  Check Android logs for WebView errors (`adb logcat | grep Chromium`).

### 视频播放失败 / Video Playback Failing

- **检查 / Check**：验证视频 URL 可从设备访问。
  Verify video URL is accessible from the device.

- **检查 / Check**：确保 ExoPlayer 支持视频格式（HLS、MP4 等）。
  Ensure ExoPlayer supports the video format (HLS, MP4, etc.).

- **检查 / Check**：检查 AndroidManifest.xml 中的网络权限。
  Check network permissions in AndroidManifest.xml.

- **检查 / Check**：验证 `NativeHttp` 桥接是否正常运行（检查 Android 日志）。
  Verify `NativeHttp` bridge is functioning (check Android logs).

### 内容未加载 / Content Not Loading

- **检查 / Check**：验证站点 URL 可达。
  Verify site URLs are reachable.

- **检查 / Check**：通过 `NativeHttp` 桥接检查 HTTP 错误。
  Check `NativeHttp` bridge for HTTP errors.

- **检查 / Check**：验证远程配置 API 中的站点配置。
  Verify site configuration in remote config API.

- **检查 / Check**：清除缓存并重试（`nc-cache.js` 清除）。
  Clear cache and retry (`nc-cache.js` clear).

### 搜索无结果 / Search Returning No Results

- **检查 / Check**：验证已配置站点启用了搜索（`"search": 1`）。
  Verify search is enabled for configured sites (`"search": 1`).

- **检查 / Check**：检查站点配置中的搜索 API 端点。
  Check search API endpoints in site configuration.

- **检查 / Check**：如果站点较慢，增加搜索超时时间。
  Increase search timeout if sites are slow.

- **检查 / Check**：验证网络连接。
  Verify network connectivity.

### 直播电视频道无法播放 / Live TV Channels Not Playing

- **检查 / Check**：验证频道流 URL 可访问。
  Verify channel stream URLs are accessible.

- **检查 / Check**：检查流格式（HLS、MP4 等）兼容性。
  Check stream format (HLS, MP4, etc.) compatibility.

- **检查 / Check**：验证 `live2cms.js` 转换是否正常工作。
  Verify `live2cms.js` conversion is working.

- **检查 / Check**：检查频道组配置。
  Check channel group configuration.

### 常见错误代码 / Common Error Codes

| 错误 / Error | 原因 / Cause | 解决方案 / Solution |
|-----------|-------------|----------|
| `NET::ERR_CERT_AUTHORITY_INVALID` | SSL 证书无效 / Invalid SSL certificate | 接受证书或使用 HTTP / Accept certificate or use HTTP |
| `NET::ERR_CACHE_MISS` | 缓存未命中 / Cache miss | 检查缓存配置 / Check cache configuration |
| `ExoPlayer ERROR_BUFFERING` | 网络缓冲 / Network buffering | 检查网络稳定性 / Check network stability |
| `ExoPlayer ERROR_UNSUPPORTED_TYPE` | 不支持的格式 / Unsupported format | 将流转换为支持的格式 / Convert stream to supported format |
| `404 Not Found` | 站点 URL 已更改 / Site URL changed | 更新站点配置 / Update site configuration |
| `403 Forbidden` | 访问被拒绝 / Access denied | 检查 API 密钥或头信息 / Check API keys or headers |

---

## API 参考 / API Reference

### NativeHttp API / NativeHttp API

`NativeHttp` 桥接使 JavaScript 能够通过 Android 网络堆栈发起 HTTP 请求。
The `NativeHttp` bridge enables JavaScript to make HTTP requests through Android's network stack.

#### 方法 / Methods

**`NativeHttp.get(url, headers)`**

- **描述 / Description**：发起 HTTP GET 请求。
  Perform an HTTP GET request.
- **参数 / Parameters**：
  - `url` (string)：请求 URL。
    Request URL.
  - `headers` (object, optional)：HTTP 头信息，以键值对形式。
    HTTP headers as key-value pairs.
- **返回 / Returns**：`Promise<string>` - 响应体作为字符串。
  Response body as string.
- **示例 / Example**：
  ```javascript
  var data = await NativeHttp.get('http://example.com/api/movies', {
      'Content-Type': 'application/json'
  });
  var json = JSON.parse(data);
  ```

**`NativeHttp.post(url, data, headers)`**

- **描述 / Description**：发起 HTTP POST 请求。
  Perform an HTTP POST request.
- **参数 / Parameters**：
  - `url` (string)：请求 URL。
    Request URL.
  - `data` (string)：请求体。
    Request body.
  - `headers` (object, optional)：HTTP 头信息，以键值对形式。
    HTTP headers as key-value pairs.
- **返回 / Returns**：`Promise<string>` - 响应体作为字符串。
  Response body as string.
- **示例 / Example**：
  ```javascript
  var response = await NativeHttp.post('http://example.com/api/search',
      JSON.stringify({keyword: 'avatar'}),
      {'Content-Type': 'application/json'}
  );
  ```

**`NativeHttp.download(url, savePath)`**

- **描述 / Description**：从 URL 下载文件并保存到本地路径。
  Download a file from URL and save to local path.
- **参数 / Parameters**：
  - `url` (string)：文件 URL。
    File URL.
  - `savePath` (string)：本地保存路径。
    Local save path.
- **返回 / Returns**：`Promise<boolean>` - 成功状态。
  Success status.

### LocalStorageBridge API / LocalStorageBridge API

`LocalStorageBridge` 将 JavaScript `localStorage` 同步到 Android `SharedPreferences`。
The `LocalStorageBridge` syncs JavaScript `localStorage` to Android `SharedPreferences`.

#### 方法 / Methods

**`LocalStorageBridge.setItem(key, value)`**

- **描述 / Description**：在 localStorage 中设置值（同步到 SharedPreferences）。
  Set a value in localStorage (synced to SharedPreferences).
- **参数 / Parameters**：
  - `key` (string)：存储键。
    Storage key.
  - `value` (string)：要存储的值。
    Value to store.
- **返回 / Returns**：`void`

**`LocalStorageBridge.getItem(key)`**

- **描述 / Description**：从 localStorage 获取值。
  Get a value from localStorage.
- **参数 / Parameters**：
  - `key` (string)：存储键。
    Storage key.
- **返回 / Returns**：`string` - 存储的值，如果未找到则为 `null`。
  Stored value, or `null` if not found.

**`LocalStorageBridge.removeItem(key)`**

- **描述 / Description**：从 localStorage 移除键。
  Remove a key from localStorage.
- **参数 / Parameters**：
  - `key` (string)：存储键。
    Storage key.
- **返回 / Returns**：`void`

**`LocalStorageBridge.clear()`**

- **描述 / Description**：清除 localStorage 中的所有数据。
  Clear all data from localStorage.
- **返回 / Returns**：`void`

**`LocalStorageBridge.keys()`**

- **描述 / Description**：获取 localStorage 中的所有键。
  Get all keys in localStorage.
- **返回 / Returns**：`string[]` - 键数组。
  Array of keys.

### ExoPlayer 包装器 API / ExoPlayer Wrapper API

`exo-player-wrapper.js` 提供了控制原生 ExoPlayer 的 JavaScript 接口。
The `exo-player-wrapper.js` provides a JavaScript interface for controlling the native ExoPlayer.

#### 方法 / Methods

**`exoPlayer.play(url)`**

- **描述 / Description**：从 URL 开始播放视频。
  Start playing a video from URL.
- **参数 / Parameters**：
  - `url` (string)：视频 URL（支持 HLS、MP4 等）。
    Video URL (supports HLS, MP4, etc.).
- **返回 / Returns**：`Promise<void>`

**`exoPlayer.pause()`**

- **描述 / Description**：暂停当前播放。
  Pause current playback.
- **返回 / Returns**：`void`

**`exoPlayer.resume()`**

- **描述 / Description**：恢复暂停的播放。
  Resume paused playback.
- **返回 / Returns**：`void`

**`exoPlayer.stop()`**

- **描述 / Description**：停止播放并释放资源。
  Stop playback and release resources.
- **返回 / Returns**：`void`

**`exoPlayer.seek(position)`**

- **描述 / Description**：跳转到指定位置。
  Seek to a specific position.
- **参数 / Parameters**：
  - `position` (number)：位置（毫秒）。
    Position in milliseconds.
- **返回 / Returns**：`void`

**`exoPlayer.getCurrentPosition()`**

- **描述 / Description**：获取当前播放位置。
  Get current playback position.
- **返回 / Returns**：`number` - 位置（毫秒）。
  Position in milliseconds.

**`exoPlayer.getDuration()`**

- **描述 / Description**：获取视频总时长。
  Get total video duration.
- **返回 / Returns**：`number` - 时长（毫秒）。
  Duration in milliseconds.

**`exoPlayer.setVolume(volume)`**

- **描述 / Description**：设置播放音量。
  Set playback volume.
- **参数 / Parameters**：
  - `volume` (number)：音量级别（0.0 到 1.0）。
    Volume level (0.0 to 1.0).
- **返回 / Returns**：`void`

**`exoPlayer.setOnCompleteListener(callback)`**

- **描述 / Description**：设置播放完成的回调。
  Set callback for playback completion.
- **参数 / Parameters**：
  - `callback` (function)：播放完成时调用的函数。
    Function to call when playback completes.
- **返回 / Returns**：`void`

**`exoPlayer.setOnErrorListener(callback)`**

- **描述 / Description**：设置播放错误的回调。
  Set callback for playback errors.
- **参数 / Parameters**：
  - `callback` (function)：出错时调用的函数。
    Function to call on error.
- **返回 / Returns**：`void`

### InPagePlayerOverlay API / InPagePlayerOverlay API

**`InPagePlayerOverlay.play(url)`**

- **描述 / Description**：启动页面内覆盖播放。
  Start in-page overlay playback.
- **参数 / Parameters**：
  - `url` (string)：视频 URL。
    Video URL.
- **返回 / Returns**：`void`

**`InPagePlayerOverlay.pause()`**

- **描述 / Description**：暂停覆盖播放。
  Pause overlay playback.
- **返回 / Returns**：`void`

**`InPagePlayerOverlay.stop()`**

- **描述 / Description**：停止并隐藏覆盖层。
  Stop and hide overlay.
- **返回 / Returns**：`void`

**`InPagePlayerOverlay.setVisible(visible)`**

- **描述 / Description**：显示或隐藏覆盖层。
  Show or hide the overlay.
- **参数 / Parameters**：
  - `visible` (boolean)：可见状态。
    Visibility state.
- **返回 / Returns**：`void`

**`InPagePlayerOverlay.setPosition(x, y)`**

- **描述 / Description**：设置覆盖层位置。
  Set overlay position.
- **参数 / Parameters**：
  - `x` (number)：X 坐标。
    X coordinate.
  - `y` (number)：Y 坐标。
    Y coordinate.
- **返回 / Returns**：`void`

**`InPagePlayerOverlay.setSize(width, height)`**

- **描述 / Description**：设置覆盖层大小。
  Set overlay size.
- **参数 / Parameters**：
  - `width` (number)：宽度（像素）。
    Width in pixels.
  - `height` (number)：高度（像素）。
    Height in pixels.
- **返回 / Returns**：`void`

---

## 许可证 / License

本项目仅供个人使用和教学目的。
This project is for personal use and educational purposes.

## 致谢 / Credits

- **ExoPlayer**：[Google ExoPlayer](https://exoplayer.dev/)
- **Android WebView**：[Android Developer](https://developer.android.com/guide/webapps/webview)
- **JavaScript**：所有 JS 模块均为本应用自定义编写。
  All JS modules are custom-written for this application.
