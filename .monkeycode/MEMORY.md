# 用户指令记忆

本文件记录了用户的指令、偏好和教导，用于在未来的交互中提供参考。

## 格式

### 用户指令条目
用户指令条目应遵循以下格式：

[用户指令摘要]
- Date: [YYYY-MM-DD]
- Context: [提及的场景或时间]
- Instructions:
  - [用户教导或指示的内容，逐行描述]

### 项目知识条目
Agent 在任务执行过程中发现的条目应遵循以下格式：

[项目知识摘要]
- Date: [YYYY-MM-DD]
- Context: Agent 在执行 [具体任务描述] 时发现
- Category: [运维部署|构建方法|测试方法|排错调试|工作流协作|环境配置]
- Instructions:
  - [具体的知识点，逐行描述]

## 去重策略
- 添加新条目前，检查是否存在相似或相同的指令
- 若发现重复，跳过新条目或与已有条目合并
- 合并时，更新上下文或日期信息
- 这有助于避免冗余条目，保持记忆文件整洁

## 条目

[项目知识摘要]
- Date: 2026-07-03
- Context: Agent 在执行 Android 个人助手项目重构时发现
- Category: 构建方法
- Instructions:
  - 项目使用 Gradle (Groovy DSL) 构建，Gradle 8.7 + AGP 8.5.2
  - 构建命令：`./gradlew :app:assembleDebug`
  - APK 输出路径：`app/build/outputs/apk/debug/app-debug.apk`
  - 当前 APK 大小约 7.8MB
  - 最低 SDK 21，目标 SDK 34，版本码 74，版本名 9.9
  - Web 资产位于 `app/src/main/assets/www/`，通过 WebViewAssetLoader 加载
  - MainActivity 中 WebView 通过 `https://personalassistant.app/www/main.html` 访问本地资源

[项目知识摘要]
- Date: 2026-07-03
- Context: Agent 在执行 Android 个人助手项目数据流分析时发现
- Category: 排错调试
- Instructions:
  - 前端使用 IndexedDB (nc-db.js) 存储采集数据，Android 层使用 Room 存储内置源
  - 数据采集通过 NativeHttp.httpGet() -> AndroidSync.httpGet() 走原生 HttpURLConnection
  - 彩票模块 SharedPreferences key 格式为 `{lotteryId}_draws`，与前端 localStorage key 一致
  - 彩票同步后前端通过 AndroidSync.saveLotteryDraw() 写回 SharedPreferences
  - AndroidJSBridge 注入的 AndroidSync 对象包含：fetchLatest(), saveLotteryDraw(), httpGet(), syncToLocalDb(), getSourcesJson(), getCategoriesJson(), getStatsJson(), initBuiltInSources()
  - 内置 9 个数据源在 MainActivity.onCreate() 时通过 executor 异步初始化到 Room

[用户指令摘要]
- Date: 2026-07-03
- Context: 用户要求每次代码修改后迭代版本号
- Instructions:
  - 每次完成代码修改后，必须递增 app/build.gradle 中的 versionCode 和 versionName
  - versionCode 为整数，每次 +1
  - versionName 为字符串，格式 X.Y.Z，每次修改至少递增最后一位
  - 每次代码修改后必须执行 ./gradlew :app:assembleDebug 构建 APK
  - 构建完成后向用户确认构建结果

[用户指令摘要]
- Date: 2026-07-03
- Context: 用户在与模型对话过程中提出语言偏好要求
- Instructions:
  - 以后所有对话回复必须使用中文

[用户指令摘要]
- Date: 2026-07-03
- Context: 用户要求每次代码修改后迭代版本号
- Instructions:
  - 每次完成代码修改后，必须递增 app/build.gradle 中的 versionCode 和 versionName
  - versionCode 为整数，每次 +1
  - versionName 为字符串，格式 X.Y.Z，每次修改至少递增最后一位
