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

[用户指令摘要]
- Date: 2026-08-03
- Context: Agent工作助手项目开发启动时明确
- Instructions:
  - 参照开发工作流完整开展开发工作
  - 严格按照任务先后依赖顺序连续开发，合理处理并行任务
  - 所有实现内容100%对齐原始开发文档，接口、数据表、业务逻辑不擅自修改
  - 每完成一大阶段，进行阶段性小结；代码结构清晰，配套必要注释
  - 若文档存在描述模糊之处，统一汇总标注，在最终末尾集中列出疑问

[用户指令摘要]
- Date: 2026-07-03
- Context: 用户在与模型对话过程中提出语言偏好要求
- Instructions:
  - 以后所有对话回复必须使用中文

[项目知识摘要]
- Date: 2026-08-03
- Context: Agent 在执行 Agent工作助手技术方案设计时发现
- Category: 工作流协作
- Instructions:
  - 参考项目 OmniBot (https://github.com/omnimind-ai/OpenOmniBot): 端侧 AI Agent、技能体系、工作区管理、记忆系统、状态机驱动
  - 参考项目 Zorv AI (https://github.com/Quor-a/ZorvAI): ACI 跨应用调用框架、多模型对话、人格系统
  - 参考项目 ArcReel (https://github.com/ArcReel/ArcReel): 多供应商抽象层、角色一致性、费用追踪
  - 技术栈选型: Kotlin + Jetpack Compose + MVVM + Hilt + Room + Retrofit
  - 多供应商架构需要抽象 TextBackend 和 ImageBackend 接口
  - 流式响应使用 Kotlin Flow 处理
  - 项目规格文档位于 .monkeycode/specs/2026-08-03-agent-work-assistant/
  - 最低 SDK 应为 29 (Android 10)，参考 OmniBot 标准
  - 使用 MMKV 替代 SharedPreferences 进行轻量存储
  - API Key 加密使用 Android Keystore + AES-256-GCM
  - 模型列表同时支持自动拉取和手动输入两种方式
  - 自动保存间隔默认 30 秒，用户可配置
  - 剧本导出仅 Markdown（PDF 暂缓）
  - 分镜角色参考图使用本地路径
  - 图片生成超时用户可配置
  - 技能仓库支持 GitHub + Gitee 双平台
  - Embedding 记忆搜索 Phase 2 实现
  - MCP 端口默认 8899，用户可配置
  - 应用仅中文，无登录系统
  - 费用统计默认美元，用户可切换人民币
  - 项目导出 ZIP 不加密
  - Phase 1 不加崩溃上报
  - 应用发布目标为本地安装（Debug/Release APK）

[项目知识摘要]
- Date: 2026-08-03
- Context: Agent 在执行 Agent工作助手项目初始化时发现
- Category: 环境配置
- Instructions:
  - Agent工作助手项目位于 /workspace/agent/
  - 使用 Kotlin + Jetpack Compose + MVVM + Hilt + Room + Retrofit 技术栈
  - 最低 SDK 29，目标 SDK 34，编译 SDK 36
  - 构建命令：`./gradlew :app:assembleDebug`
  - APK 输出路径：`app/build/outputs/apk/debug/app-debug.apk`
  - 每次代码修改后必须执行 ./gradlew :app:assembleDebug 构建 APK
  - 构建完成后向用户确认构建结果
