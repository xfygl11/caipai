# 个人助手 Android APP

基于 Android WebView 的个人全能助手应用，集成影视点播、彩票预测、数据采集、本地数据库等功能。

## 功能

- 影视点播: 9 个内置采集源、分类浏览、本地采集、HLS 播放、收藏历史
- 彩票预测: 大乐透、双色球、七乐彩、福彩3D、排列3/5、7星彩、快乐8、七星彩
- 数据采集: 支持 9 个内置数据源一键采集，IndexedDB 本地存储
- 彩票同步: 联网同步开奖数据，SharedPreferences 本地缓存
- 数据库查看: 支持源/分类筛选、关键词搜索、卡片式展示
- 我的页面: 收藏统计、历史记录、影片总数统计
- 内置 Room 数据库: 持久化存储采集源、分类、影片数据

## 版本

当前版本: v10.7 (versionCode 82)

## GitHub Actions 自动构建 APK

推送代码到 GitHub 后自动构建:

1. 点击仓库页面的 `Actions`
2. 选择 `Build Android APK`
3. 点击 `Run workflow`
4. 构建完成后在 Artifacts 下载 APK

## 本地构建

### 前置条件

- JDK 17
- Android SDK (API 34)
- Gradle 8.7

### 构建步骤

```bash
./gradlew assembleDebug
```

APK 输出路径: `app/build/outputs/apk/debug/app-debug.apk`

## 项目结构

```
├── app/
│   ├── build.gradle              # App 模块构建配置（含签名）
│   └── src/main/
│       ├── AndroidManifest.xml    # Android 清单
│       ├── java/
│       │   ├── MainActivity.java       # WebView 主活动
│       │   ├── bridge/AndroidJSBridge.java  # JS-Native 桥接
│       │   ├── db/                     # Room 数据库
│       │   │   ├── AppDatabase.java
│       │   │   ├── dao/
│       │   │   └── entity/
│       │   └── util/HttpUtil.java      # OkHttp 网络请求
│       ├── res/                   # Android 资源
│       └── assets/www/           # Web 前端资源
│           ├── main.html
│           ├── assets/js/        # 前端 JS 模块
│           ├── assets/css/       # 样式表
│           └── assets/player/    # 播放器库
├── build.gradle                   # 根项目构建配置
├── settings.gradle                # 项目设置
├── gradle.properties              # Gradle 属性
├── release.keystore               # 签名密钥
├── .github/workflows/build-apk.yml  # CI/CD 工作流
└── .monkeycode/                   # 项目管理目录
    └── MEMORY.md                  # 项目记忆
```

## 技术栈

- **Android**: Java, WebView, Room Database, OkHttp
- **前端**: 原生 HTML/CSS/JS, IndexedDB
- **构建**: Gradle 8.7, AGP 8.5.2
- **最低版本**: Android 5.0 (API 21)

## 数据存储

- **包名**: `com.personalassistant.app`
- **Room 数据库**: `personal_assistant_db`
- **IndexedDB**: `NewCloudDB`
- **SharedPreferences**: 彩票开奖数据缓存

升级安装不会清除数据，只有卸载才会删除。
