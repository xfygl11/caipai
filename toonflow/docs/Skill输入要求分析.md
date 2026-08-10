## Skill输入要求分析总结

### 剧本创作阶段

#### 阶段0: 项目初始化 (script_agent_decision.md)
**用户需要输入:**
| 参数 | 说明 | 示例 |
|------|------|------|
| 项目标题 | 短剧名称 | 《凌玄传》 |
| 计划集数 | 总共拆分为几集 | 20集 |
| 单集时长 | 每集目标时长（分钟） | 3分钟 |
| 平台规格 | 画面比例 | 竖屏9:16 |
| 风格定位 | 短剧整体风格标签 | 战神逆袭 |
| 付费策略 | 前几集免费 | 前3集免费 |
| 原著范围 | 改编覆盖的章节范围 | 第1-10章 |

**自动计算:**
- 台词字数 = 单集时长 × 150字/分钟 = 3 × 150 = **450字/集**

---

#### 阶段1: 故事骨架 (script_execution_skeleton.md)
**用户输入:** 无（自动读取项目配置）

**工具调用:**
- get_planData - 确认工作区状态
- get_novel_events(ids) - 获取事件表

**输出:** storySkeleton XML

---

#### 阶段2: 改编策略 (script_execution_adaptation.md)
**用户输入:** 无（自动读取项目配置和故事骨架）

**工具调用:**
- get_novel_events(ids) - 获取事件表
- get_planData - 获取故事骨架

**输出:** adaptationStrategy XML

---

#### 阶段3: 剧本编写 (script_execution_script.md)
**用户需要输入:**
| 参数 | 说明 |
|------|------|
| 当前集数 | 要编写的集数（第1集、第2集...） |
| 生成集数 | 本次生成几集（默认3集，最多5集） |

**自动读取:**
- 单集时长（从项目配置）
- 画风（从项目配置）
- 目标章节（从故事骨架）

**工具调用:**
- get_planData - 获取骨架与改编策略
- get_script_content(ids) - 获取上一集剧本
- get_novel_text - 获取原文
- get_novel_events(ids) - 获取事件表

**输出:** scriptItem XML

---

### 视频制作阶段

#### 阶段1: 导演规划 (production_execution_director_plan.md)
**用户输入:** 无（自动读取剧本和资产）

**工具调用:**
- get_flowData("script") - 读取剧本
- get_flowData("assets") - 读取资产

**输出:** scriptPlan XML

---

#### 阶段2: 衍生资产分析 (production_execution_derive_assets.md)
**用户输入:** 无（自动分析）

**展示给用户:**
- 衍生资产清单（需要用户确认）

**工具调用:**
- get_flowData("script") - 读取剧本
- get_flowData("assets") - 读取资产
- add_deriveAsset - 写入衍生资产

**用户决策:**
- 确认全部生成
- 部分生成
- 跳过

---

#### 阶段3: 衍生资产生成 (production_execution_generate_assets.md)
**用户输入:** 衍生资产清单（来自阶段2确认）

**工具调用:**
- get_flowData("assets") - 读取资产列表
- generate_assets_images - 生成图片（异步）

---

#### 阶段4: 构建分镜表 (production_execution_storyboard_table.md)
**用户输入:** 无（自动读取）

**工具调用:**
- get_flowData("script") - 读取剧本
- get_flowData("assets") - 读取资产
- get_flowData("scriptPlan") - 读取导演规划

**输出:** storyboardTable XML

---

#### 阶段5: 分镜面板写入 (production_execution_storyboard_panel.md)
**用户输入:** 无（自动读取）

**工具调用:**
- get_flowData("script") - 读取剧本
- get_flowData("storyboardTable") - 读取分镜表
- add_flowData_storyboard - 写入分镜面板

---

#### 阶段6: 分镜图生成 (production_execution_storyboard_gen.md)
**用户输入:** 无（自动读取）

**工具调用:**
- get_flowData("storyboard") - 读取分镜面板
- generate_storyboard_images - 生成分镜图（异步）

---

## UI更新内容

### 项目初始化页面
- 添加新建项目弹窗
- 包含所有必填参数输入框

### 剧本生成页面
- 添加"生成集数"选择（1集/3集/5集）

### 导演规划页面
- 添加"生成导演规划"按钮
- 说明：基于剧本自动拆分场次

### 分镜表页面
- 更新为"分镜表生成"
- 添加"生成分镜表"按钮
- 说明：基于导演规划自动生成
