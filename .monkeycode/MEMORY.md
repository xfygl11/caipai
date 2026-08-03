# 用户指令记忆

本文件记录用户指令、偏好和教学，供未来交互参考。

## 格式

### 用户指令条目
用户指令条目应遵循以下格式：

[用户指令摘要]
- 日期: [YYYY-MM-DD]
- 上下文: [提及的场景或时间]
- 指令:
  - [用户教学或指令的内容，逐行描述]

### 项目知识条目
Agent 在任务执行过程中发现的项目知识应遵循以下格式：

[项目知识摘要]
- 日期: [YYYY-MM-DD]
- 上下文: Agent 在执行 [具体任务描述] 时发现
- 分类: [操作与部署|构建方法|测试方法|故障排查与调试|工作流与协作|环境配置]
- 指令:
  - [具体知识点，逐行描述]

## 去重策略
- 添加新条目前，检查是否有相似或相同的指令。
- 如果发现重复，跳过新条目或与现有条目合并。
- 合并时，更新上下文或日期信息。
- 这有助于避免冗余条目，保持记忆文件整洁。

## 条目

[项目知识摘要]
- 日期: 2026-08-03
- 上下文: Agent 开发 Agent 工作助手 Android 应用的工作流程与进度
- 分类: 构建方法
- 指令:
  - 使用 Gradle 8.7 配合 Java 17 进行 Android 开发
  - Android SDK 必须安装在 /usr/lib/android-sdk
  - 在 CI/CD 环境中使用 --no-daemon 标志运行构建
  - Netty 依赖冲突需要在 packaging options 中排除 META-INF/INDEX.LIST
  - compileSdk 使用 34 以兼容 AGP 8.2.0
  - APK 输出路径: app/build/outputs/apk/debug/app-debug.apk (59MB)
  - 项目共 81 个 Kotlin 文件
  - main 分支共有 5 个 Git 提交

[项目知识摘要]
- 日期: 2026-08-03
- 上下文: Agent 工作助手 Android 应用开发过程中的 Room 数据库迁移问题
- 分类: 故障排查与调试
- 指令:
  - 使用 @ColumnInfo(name = "column_name") 处理 snake_case 数据库列名
  - SQL 查询中保留字如 `order` 需使用反引号包裹
  - 外键列必须添加对应索引以提升性能

[项目知识摘要]
- 日期: 2026-08-03
- 上下文: Agent 工作助手 Android 应用的 Hilt 依赖注入配置
- 分类: 构建方法
- 指令:
  - 所有 DAO 必须通过 AppModule 中的 @Provides 方法提供
  - 数据库实例应在 AppModule 中创建并提供
  - 每个 DAO 都需要显式的 @Provides 绑定以支持 Hilt 注入

[项目知识摘要]
- 日期: 2026-08-03
- 上下文: Agent 工作助手项目开发进度和构建信息
- 分类: 构建方法
- 指令:
  - 项目已完成基础架构搭建 (Phase 1-8)
  - APK路径: app/build/outputs/apk/debug/app-debug.apk (59MB)
  - 共82个Kotlin文件，16个Git提交
  - 已实现: 项目管理、聊天功能、小说创作、剧本创作、分镜制作
  - 待实现: 图片生成、技能商店、工作区管理完整UI
  - 构建命令: /tmp/gradle-8.7/bin/gradle :app:assembleDebug --no-daemon
  - 测试命令: /tmp/gradle-8.7/bin/gradle testDebugUnitTest --no-daemon

[用户指令摘要]
- 日期: 2026-08-03
- 上下文: 用户明确要求所有输出使用中文
- 指令:
  - 所有输出必须使用简体中文显示，包括思考过程、代码注释、报告文档等
  - 不得在回复中使用英文，所有文本内容均需翻译为中文
  - 技术术语可保留英文，但解释说明必须使用中文
