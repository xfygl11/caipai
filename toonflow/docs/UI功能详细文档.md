# Toonflow-app UI功能使用过程详细文档

> 本文档详细说明每个UI功能模块的使用过程和背后调用的技能/Agent

---

## 一、小说管理模块

### 1.1 事件提取功能

**用户操作流程**:
1. 进入"小说管理"页面
2. 选择要提取事件的章节
3. 点击"提取事件"按钮
4. 系统后台调用 `generateEvents.ts`
5. 使用 `event_extraction` 技能
6. 事件列表显示在页面

**后端实现**:
```typescript
// src/routes/novel/event/generateEvents.ts
new u.cleanNovel(concurrentCount).start(allChapters, projectId)
// → 调用 event_extraction 技能
// → 输出 JSON 格式事件列表
```

**技能文件**: `data/skills/event_extraction.md`

---

### 1.2 小说章节生成

**用户操作流程**:
1. 选择"生成章节"功能
2. 输入章节大纲或提示
3. 系统调用 `novel_chapter` 技能
4. 流式输出章节内容

**技能文件**: `data/skills/novel_chapter.md`

---

## 二、剧本创作模块

### 2.1 故事骨架搭建

**用户操作流程**:
1. 进入"剧本创作"页面
2. 点击"搭建故事骨架"
3. 系统调用 `scriptAgent:storySkeletonAgent`
4. 使用技能: `script_execution_skeleton.md`

**技能内容**:
- 故事核设计 (一句话总结 + 爽点 + 金手指)
- 人物小传 (≤4人，大三角核心角色)
- 三幕结构 (每幕功能、核心问题、覆盖章节)
- 付费卡点设计 (按10%/30%/50%/70%/90%比例)
- 股价级反转设计 (全剧约3个)

**输出格式**:
```xml
<storySkeleton>
  <!-- 故事核 -->
  <!-- 人物小传 -->
  <!-- 三幕结构 -->
  <!-- 付费卡点 -->
  <!-- 反转登记表 -->
</storySkeleton>
```

---

### 2.2 改编策略制定

**用户操作流程**:
1. 故事骨架完成后
2. 点击"制定改编策略"
3. 系统调用 `scriptAgent:adaptationStrategyAgent`
4. 使用技能: `script_execution_adaptation.md`

**功能**:
- 分析小说与剧本的差异
- 制定删减/改编决策
- 输出改编策略文档

---

### 2.3 剧本生成

**用户操作流程**:
1. 骨架和策略完成后
2. 点击"生成剧本"
3. 系统调用 `scriptAgent:scriptAgent`
4. 使用技能: `script_execution_script.md`

**剧本格式**:
```xml
<scriptItem name="第1集">
  剧本内容...
</scriptItem>
```

---

### 2.4 剧本资产提取

**用户操作流程**:
1. 剧本生成完成后
2. 点击"提取资产"
3. 系统调用 `extractAssets.ts`
4. 批量处理剧本 (每5集一组)
5. AI识别新资产和已有资产引用

**后端实现**:
```typescript
// src/routes/script/extractAssets.ts
const result = await u.Ai.Text("universalAi").generate({
  tools: { resultTool },
  // 输入: 剧本内容 + 已有资产列表
  // 输出: 新资产列表 + 已有资产引用
})
```

**资产类型**:
- `role`: 角色资产
- `tool`: 道具资产
- `scene`: 场景资产

---

## 三、视频生产模块

### 3.1 衍生资产分析

**用户操作流程**:
1. 进入"视频生产"页面
2. 点击"分析衍生资产"
3. 系统调用 `productionAgent:deriveAssetsAgent`
4. 使用技能: `production_execution_derive_assets.md`

**功能**:
- 分析剧本需要的资产
- 与已有资产对比
- 标记需要生成的资产

---

### 3.2 衍生资产图片生成

**用户操作流程**:
1. 资产分析完成后
2. 点击"生成资产图片"
3. 系统调用 `productionAgent:generateAssetsAgent`
4. 使用技能: `production_execution_generate_assets.md`

**后端实现**:
```typescript
// 调用生成API
generate_assets_images({ ids: [资产id列表] })
// 异步生成，发起即返回
```

---

### 3.3 导演规划

**用户操作流程**:
1. 资产生成完成后
2. 点击"导演规划"
3. 系统调用 `productionAgent:directorPlanAgent`
4. 使用技能: `production_execution_director_plan.md`

**功能**:
- 拆分场次 (同一时空连续戏)
- 统计台词数量和字数
- 分析情绪浓度 (0-10)
- 设计场间过渡

**输出格式**:
```xml
<scriptPlan>
  <scenes>
    <scene id="Sc1" name="客厅·夜晚" lines="5" words="120" emotion="7" tone="紧张对峙"/>
  </scenes>
  <notes>
    <scene id="Sc1">
      <emotionalPoint>男主发现真相时的震惊表情</emotionalPoint>
      <consistencyAnchor>男主穿着黑色西装</consistencyAnchor>
    </scene>
  </notes>
  <transitions>
    <transition from="Sc1" to="Sc2" type="空镜过渡">雨夜街道空镜 → 淡入下一场</transition>
  </transitions>
</scriptPlan>
```

---

### 3.4 分镜表生成

**用户操作流程**:
1. 导演规划完成后
2. 点击"生成分镜表"
3. 系统调用 `productionAgent:storyboardTableAgent`
4. 使用技能: `production_execution_storyboard_table.md`

**核心规则**:
- 每个片段≤15秒
- 长台词强制拆镜 (超过20字)
- 台词零删改
- 出场人物完整

**输出格式**:
```xml
<storyboardTable>
  <scene id="1" name="客厅·夜晚">
    <characters>苏晚卿, 凌玄</characters>
    <assetIds>101, 100</assetIds>
    <fragment duration="12s">
      <shots>
        <shot index="1" duration="5" shotType="近景" camera="缓推">
          <description>苏晚卿紧握拳头，指节泛白</description>
          <dialogue>『你到底为什么要骗我？』</dialogue>
        </shot>
      </shots>
    </fragment>
  </scene>
</storyboardTable>
```

---

### 3.5 分镜面板生成

**用户操作流程**:
1. 分镜表生成完成后
2. 点击"生成分镜图"
3. 系统调用 `productionAgent:storyboardPanelAgent`
4. 使用技能: `production_execution_storyboard_panel.md`

**功能**:
- 为每个分镜生成图片提示词
- 调用图像生成API
- 轮询生成状态

---

### 3.6 视频生成

**用户操作流程**:
1. 分镜图片完成后
2. 进入"视频工作bench"
3. 点击"生成视频"
4. 系统调用 `generateVideo` 接口
5. 使用视频生成模型
6. 轮询视频生成状态

**后端实现**:
```typescript
// src/routes/production/workbench/generateVideo.ts
generateVideo({
  prompt: "视频描述",
  model: project.videoModel,
  // ...
})
// 异步生成，返回 taskId
// 前端轮询 checkVideoStateList
```

---

## 四、技能系统详解

### 4.1 技能加载流程

**文件**: `src/utils/agent/skillsTools.ts`

**流程**:
```
1. 解析 frontmatter
   ├── name: 技能名称
   └── description: 技能描述

2. 扫描技能目录
   ├── workspace: 二级技能目录
   └── attachedSkills: 三级技能目录 (递归)

3. 创建工具函数
   ├── activate_skill: 激活技能
   └── read_skill_file: 读取资源文件
```

### 4.2 技能激活机制

**主技能**:
- `script_agent_decision` - 剧本决策
- `script_execution_skeleton` - 故事骨架
- `script_execution_adaptation` - 改编策略
- `script_execution_script` - 剧本生成
- `script_agent_supervision` - 剧本监督
- `production_agent_decision` - 生产决策
- `production_agent_execution` - 生产执行
- `production_agent_supervision` - 生产监督

**激活命令**:
```typescript
activate_skill({ name: "script_execution_skeleton" })
// → 返回技能完整内容和资源文件列表
```

---

## 五、记忆系统

### 5.1 记忆类型

**文件**: `src/utils/agent/memory.ts`

| 类型 | 说明 |
|------|------|
| `rag` | 向量检索记忆 (RAG) |
| `summaries` | 历史摘要 |
| `shortTerm` | 短期对话 |

### 5.2 记忆构建

```typescript
function buildMemPrompt(mem) {
  let context = ""
  if (mem.rag.length) {
    context += `[相关记忆]\n${mem.rag.map(r => r.content).join("\n")}`
  }
  if (mem.summaries.length) {
    context += `[历史摘要]\n${mem.summaries.map((s, i) => `${i+1}. ${s.content}`).join("\n")}`
  }
  if (mem.shortTerm.length) {
    context += `[近期对话]\n${mem.shortTerm.map(m => `${m.role}: ${m.content}`).join("\n")}`
  }
  return `## Memory\n${context}`
}
```

---

## 六、AI模型配置

### 6.1 模型选择

**文件**: `src/utils/ai.ts`

**模型类型**:
- `scriptAgent` - 剧本Agent整体
- `productionAgent` - 生产Agent整体
- `universalAi` - 通用AI
- `scriptAgent:decisionAgent` - 剧本决策
- `scriptAgent:storySkeletonAgent` - 故事骨架
- `scriptAgent:adaptationStrategyAgent` - 改编策略
- `scriptAgent:scriptAgent` - 剧本生成
- `productionAgent:directorPlanAgent` - 导演规划
- `productionAgent:storyboardTableAgent` - 分镜表

**配置方式**:
1. 简易模式: 所有子Agent使用同一模型
2. 高级模式: 每个子Agent可独立配置模型

---

## 七、UI功能与技术对应表

| UI功能 | 路由 | 技能/Agent | 描述 |
|--------|------|-----------|------|
| 提取事件 | `/api/novel/event/generate` | `event_extraction` | 从小说提取关键事件 |
| 生成章节 | `/api/novel/generateChapter` | `novel_chapter` | 生成小说章节 |
| 搭建骨架 | `/api/scriptAgent/plan` | `script_execution_skeleton` | 构建故事骨架 |
| 制定策略 | `/api/scriptAgent/plan` | `script_execution_adaptation` | 制定改编策略 |
| 生成剧本 | `/api/scriptAgent/plan` | `script_execution_script` | 生成完整剧本 |
| 提取资产 | `/api/script/extractAssets` | 内置工具 | 从剧本提取角色/场景/道具 |
| 导演规划 | `/api/production/workbench/getFlowData` | `production_execution_director_plan` | 拆分场次、分析情绪 |
| 分镜表 | `/api/production/workbench/getFlowData` | `production_execution_storyboard_table` | 生成详细分镜 |
| 分镜图 | `/api/production/workbench/getFlowData` | `production_execution_storyboard_panel` | 生成单镜图片 |
| 资产生成 | `/api/assetsGenerate/generate` | `production_execution_generate_assets` | 生成资产图片 |
| 视频生成 | `/api/production/workbench/generateVideo` | 内置工具 | 生成视频片段 |

---

## 八、数据流向图

```
小说 → 事件提取 → 故事骨架 → 改编策略 → 剧本 → 资产提取
                                              ↓
                                          导演规划
                                              ↓
                                          分镜表 → 分镜图
                                              ↓
                                          资产生成 → 资产图片
                                              ↓
                                          视频生成
```

---

*文档完成时间: 2026-08-09*
