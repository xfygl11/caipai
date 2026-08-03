# OmniBot 能力分析报告

## 项目信息

- **名称**: OmniBot (OpenOmniBot)
- **GitHub**: https://github.com/omnimind-ai/OpenOmniBot
- **文档**: https://omnimind-ai.github.io/OmniBot-Docs/
- **技术栈**: Kotlin + Flutter 混合架构
- **星标**: 2000+
- **最小 SDK**: 29 (Android 10)

## 核心能力

### 1. AI Chat 与 Agent 执行
- 对话线程与归档
- 工具活动条（执行可视化）
- 深度思考卡片
- 执行摘要卡片
- 浏览器叠层
- 工件预览

### 2. 手机自动化与 VLM 任务
- `accessibility` 读取界面树
- `MediaProjectionForegroundService` 负责屏幕采集
- `assists` 维护任务生命周期
- 场景模型中的 `scene.vlm.operation.primary` 负责核心视觉决策

### 3. 权限与常驻能力
- 后台运行权限
- 悬浮窗权限
- 应用列表读取
- 无障碍服务
- 可选的 Shizuku
- 精确闹钟与前台服务

### 4. 技能体系
- 技能商店页支持搜索、安装、启用/禁用、删除
- 从仓库链接直接安装技能
- 推荐仓库: https://github.com/OpenMinis/MinisSkills

### 5. 工作区、文件与终端
- 文件浏览
- Artifact 预览
- Workspace 路径映射
- 嵌入式终端运行时
- 浏览器文件选择与共享链路

### 6. 本地模型与混合推理
- **远端**: OpenAI 兼容协议、Anthropic
- **本地**: llama.cpp、omniinfer-mnn、executorch-qnn

### 7. 记忆系统
- `SOUL.md`: 角色与行为边界
- `CHAT.md`: 对话默认提示
- `MEMORY.md`: 长期稳定记忆
- 短期记忆文件
- embedding 配置
- nightly rollup

### 8. 定时任务与提醒
- `Scheduled task`: 可执行的任务
- `Alarm`: 提醒类能力
- 子 Agent 流程

### 9. MCP 与 WebChat
- 局域网 MCP 服务（端口 8899）
- Bearer Token 鉴权
- WebChat 页面
- Workspace 文件访问
- Browser Mirror
- Conversation API

### 10. 远程 Codex Bridge
- 手机端使用 PC 上的 Codex CLI
- 通过 codex-bridge 实现局域网连接

## 架构概览

```
OpenOmniBot/
├── app/                 # Android 主宿主模块：入口、Agent 编排、系统能力、MCP、前台服务
├── ui/                  # Flutter Android UI 模块：聊天、设置、任务和记忆
├── webchat/             # React + TypeScript WebUI；由 Vite 构建并通过 Android 打包
├── baselib/             # 基础核心库：数据库、存储、网络、模型配置、权限等
├── assists/             # 公共任务生命周期与聊天/模型协调
├── uikit/               # 原生浮层 UI：悬浮球、覆盖层面板、半屏界面
└── ReTerminal/core/     # 内嵌终端体验相关模块
```

## 关键文件

- `app/src/main/java/cn/com/omnimind/bot/App.kt`: Application 入口点与 MCP 集成
- `assists/src/main/java/cn/com/omnimind/assists/StateMachine.kt`: 任务状态机
- `assists/src/main/java/cn/com/omnimind/assists/AssistsCore.kt`: 任务 SDK 接口
- `baselib/src/main/java/cn/com/omnimind/baselib/database/`: 数据库层

## 场景模型绑定

| 场景 | 用途 | 推荐模型类型 |
|------|------|-------------|
| `scene.dispatch.model` | 任务理解与分流 | 综合能力强的模型 |
| `scene.voice` | 语音场景 | 语音模型 |
| `scene.vlm.operation.primary` | UI 操作主链路 | 多模态/视觉模型 |
| `scene.compactor.context` | 上下文压缩 | 总结类模型 |
| `scene.compactor.context.chat` | 聊天上下文压缩 | 轻量模型 |
| `scene.loading.sprite` | 加载动画 | - |
| `scene.memory.embedding` | 记忆嵌入 | 必须用 embedding 模型 |
| `scene.memory.rollup` | 夜间记忆整理 | 成本更低的总结模型 |

## 工作区结构

```
workspace/.omnibot/
├── agent/config.json
├── memory/SOUL.md
├── memory/CHAT.md
├── memory/MEMORY.md
├── memory/short-memories/
├── models/
│   ├── OmniInfer-llama
│   ├── OmniInfer-mnn
│   └── OmniInfer-qnn
└── ...
```

## 构建命令

```bash
# 获取源码
git clone https://github.com/omnimind-ai/OpenOmniBot.git
cd OpenOmniBot

# 初始化子模块
git submodule update --init third_party/omniinfer

# 初始化 Flutter
cd ui && flutter pub get

# 构建调试包
./gradlew assembleDevelopDebug

# 安装到设备
./gradlew installDevelopDebug
```

## 环境要求

- JDK 11+
- Flutter SDK 3.9.2+
- Android SDK compileSdk 36
- Node.js 18+ (仅用于文档站)
