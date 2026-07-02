# 个人助手 Android APP

基于 Android WebView 的个人全能助手应用，集成影视播放、彩票预测、三角洲游戏币计算器等功能。

## 功能

- 影视点播: 多源采集、分类浏览、HLS 播放、收藏历史
- 彩票预测: 大乐透、双色球、七乐彩、福彩3D、排列3/5、7星彩、快乐8
- 三角洲计算器: 游戏币/子弹/防具价值计算
- 本地数据库: IndexedDB 存储采集数据和用户配置

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

### 构建步骤

```bash
./gradlew assembleDebug
```

APK 输出路径: `app/build/outputs/apk/debug/app-debug.apk`

## 项目结构

```
├── app/
│   ├── build.gradle              # App 模块构建配置
│   └── src/main/
│       ├── AndroidManifest.xml    # Android 清单
│       ├── java/.../MainActivity.java  # WebView 主活动
│       ├── res/                   # Android 资源
│       └── assets/www/           # Web 前端资源
├── build.gradle                   # 根项目构建配置
├── settings.gradle                # 项目设置
├── gradle.properties              # Gradle 属性
└── .github/workflows/build-apk.yml  # CI/CD 工作流
```
