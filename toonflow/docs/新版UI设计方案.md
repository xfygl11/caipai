Toonflow Android 完整版UI设计规范文档
 
文档概述
 
本文基于现有 UI_COMPONENTS.md 基础规范，结合项目架构、Agent业务流程、数据库实体、页面路由体系，输出全链路、可落地、含业务场景的完整UI设计方案，覆盖设计基础系统、原子组件、分子/有机体组件、完整页面布局、交互流程、状态规范、多端适配、深色/浅色模式、动效、弹窗/浮层、无障碍规范、业务场景适配十一大模块，可直接交付UI设计师与Compose开发落地。
 
一、基础设计系统（底层统一约束）
 
1.1 色彩系统（扩展完整业务色值+使用场景）
 
遵循Material3双模式（Light/Dark），补充业务语义色、功能色、状态色，统一标记 MaterialTheme.colorScheme 映射
 
语义令牌 Light色值 Dark色值 业务使用场景 
primary #6366F1 #8B8AFF 主按钮、Tab激活态、标题强调、进度条主色、生成按钮、项目高亮 
primaryContainer #E0E7FF #312E81 卡片高亮背景、输入框激活底色、选中项背景、技能标签底色 
onPrimary #FFFFFF #111827 主色容器内文字/图标 
secondary #EC4899 #F472B6 次要操作、衍生资产标签、分镜标记、AI助手标识 
secondaryContainer #FCE7F3 #501738 次要功能卡片、对话气泡AI侧背景 
tertiary #F59E0B #FBBF24 警告、待处理任务、未提取章节、加载提示 
tertiaryContainer #FFFBEB #422006 警告提示框、任务队列待执行项背景 
surface #FFFFFF #1E1E2E 卡片、弹窗、底部导航、输入框容器 
surfaceVariant #F3F4F6 #313244 列表项、分割区域、次要卡片、AI消息气泡底色 
background #F8F8FC #181825 页面全局背景 
onBackground #111827 #E5E7EB 页面正文文字 
onSurface #1F2937 #F3F4F6 卡片内正文 
onSurfaceVariant #6B7280 #9CA3AF 辅助文字、说明、标签、时间、次要描述 
error #EF4444 #F87171 删除、失败、报错、停止按钮、异常状态 
errorContainer #FEE2E2 #450A0A 错误提示框、失败任务背景 
success #22C55E #4ADE80 完成、已渲染、保存成功、资产入库、导出完成 
successContainer #DCFCE7 #14532D 成功提示、已完成任务卡片底色 
outline #D1D5DB #4B5563 分割线、输入框边框、普通卡片描边 
outlineVariant #E5E7EB #374151 弱分割、次要组件边框 
shadow rgba(0,0,0,0.12) rgba(0,0,0.35) 卡片阴影、弹窗遮罩、浮层阴影 
 
业务专属扩展色（无M3映射，仅业务标签使用）
 
业务标签 Light Dark 用途 
novelTag #06B6D4 #22D3EE 小说模块、章节标签 
scriptTag #8B5CF6 #A78BFA 剧本、故事骨架、分镜脚本 
assetTag #10B981 #34D399 全局资产、角色/场景/道具 
episodeTag #F97316 #FB923C 单集资产、本集衍生素材 
agentTag #84CC16 #A3E635 AI智能体、技能模块 
 
1.2 字体排版系统（完善业务场景字号权重）
 
统一使用系统无衬线字体，所有尺寸单位sp，行高dp，严格区分标题/正文/辅助文字/标签
 
层级 字号sp 字重 行高dp 使用场景 
Display Large 57 400 64 空页面大标题、欢迎页主标题 
Display Medium 45 400 52 弹窗顶部大标题、项目名称超大展示 
Display Small 36 400 44 模块分区大标题、统计数字 
Headline Large 32 400 40 页面主标题（项目列表顶部） 
Headline Medium 28 400 36 弹窗标题、功能分区标题 
Headline Small 24 400 32 卡片头部标题、模块副标题 
Title Large 22 500 28 项目卡片名称、剧本集数标题 
Title Medium 16 500 24 功能卡片标题、Tab栏文字、输入框标签 
Title Small 14 500 20 列表条目主文字、技能名称、任务标题 
Body Large 16 400 24 正文内容、AI对话文本、长描述 
Body Medium 14 400 20 卡片辅助描述、参数说明、章节简介 
Body Small 12 400 16 备注、补充说明、提示文字 
Label Large 14 500 20 操作按钮文字、Chip标签、复选框文本 
Label Medium 12 500 16 状态标签、时间、计数、小按钮文字 
Label Small 11 500 16 极小提示、版本号、底部备注、水印文字 
 
1.3 间距系统（补充业务场景间距规则）
 
全部单位dp，统一全局间距Token，禁止写死数值
 
Token 值 适用场景 
xs 4 组件内部极小留白、文字与图标间隙、列表内文字间距 
sm 8 卡片内元素间距、按钮内边距、列表项上下内边距、图标与文字间距 
md 12 模块分割间距、弹窗内分组间距、输入框上下边距 
lg 16 页面左右安全边距、卡片整体padding、页面模块上下间距 
xl 24 页面大分区间隔、弹窗顶部底部留白、页面标题与内容间距 
xxl 32 页面顶部大留白、空状态组件上下间距 
xxxl 48 欢迎页、全屏空状态垂直留白 
 
页面全局边距强制规则
 
1. 所有页面内容左右基础边距： lg=16dp 
2. 顶部TopAppBar下方内容首元素上边距： lg=16dp 
3. 底部导航上方内容底部预留： 80dp （含导航+安全区）
4. 弹窗内部左右边距统一 lg=16dp 
 
1.4 圆角系统（完整组件圆角绑定）
 
Token 值 绑定组件 
extraSmall 4dp 分割标签、小Chip、输入框边角、小型进度条 
small 8dp 按钮、下拉选项、小卡片、技能标签 
medium 12dp 列表卡片、输入框、弹窗内子模块、分镜单元格 
large 16dp 主功能卡片、消息气泡、弹窗主体、项目卡片 
extraLarge 24dp 全屏弹窗、大浮层、首页超大功能模块 
full 999dp 圆形头像、圆形FAB、圆形Icon按钮、进度环形 
 
1.5 阴影与高程规范
 
采用M3标准高程分层，统一阴影参数
 
高程 组件类型 阴影表现 
0dp 静态页面背景、分割线、普通文字 无阴影 
1dp 普通列表、静态卡片、只读面板 极浅阴影，仅区分层级 
2dp 项目卡片、功能卡片、对话气泡 常规卡片阴影 
4dp 底部输入框、底部导航、弹窗、浮层 中等阴影，悬浮感 
6dp FAB、悬浮任务按钮、下拉菜单、弹出菜单 强悬浮阴影 
12dp 全屏弹窗、模态浮层、独立对话框 重度阴影，隔离底层页面 
 
1.6 安全区适配规范
 
1. 顶部状态栏：TopAppBar高度56dp，自动避让刘海/挖孔
2. 底部导航：增加 WindowInsets.navigationBars 底部padding，适配安卓手势条
3. 弹窗、全屏组件统一使用 WindowInsets 填充安全区，不出现内容被裁切
 
二、原子组件（最小基础单元，全局复用）
 
2.1 图标组件 Icon
 
1. 统一使用Material Icons，分三种尺寸：
- Small：20dp（标签、列表内图标）
- Medium：24dp（按钮、卡片头部）
- Large：32dp（功能卡片主图标、空状态图标）
2. 颜色规则：默认 onSurfaceVariant ；激活态 primary ；危险操作 error ；成功 success 
3. 点击图标统一使用 IconButton ，最小点击区域48dp×48dp（防触摸过小）
 
2.2 文本组件 Text
 
封装全局通用Text，强制绑定Typography，禁止自定义字号字重
扩展参数：maxLines、overflow、color、textAlign，支持单行省略、多行截断
 
2.3 按钮体系（完整分层）
 
2.3.1 FilledButton 填充主按钮
 
- 圆角：small=8dp / large=16dp
- 背景：primary，文字onPrimary
- 场景：新建项目、生成小说、生成剧本、资产入库、确认操作
- 禁用态：透明度40%，不可点击
 
2.3.2 OutlinedButton 描边次要按钮
 
- 边框：outline，文字primary
- 场景：导出、预览、复制、刷新、查看详情
 
2.3.3 TextButton 文字按钮
 
无背景无边框，仅文字可点击
场景：取消、返回、编辑、删除、查看更多
 
2.3.4 IconButton 图标按钮
 
圆形full圆角，48dp点击区域
场景：附件、发送、停止、关闭弹窗、删除单行数据
 
2.3.5 AssistChip / FilterChip 标签按钮
 
1. AssistChip：静态标签，不可点击（题材、画风、状态标记）
2. FilterChip：可单选/多选（模型选择、资产分类、章节筛选）
圆角full，高度32dp，内部padding sm=8dp
 
2.4 输入框 OutlinedTextField
 
统一圆角medium=12dp，maxLines支持1~4行
支持状态：普通/聚焦/错误/禁用
辅助功能：placeholder、label、errorText、尾部图标清除按钮
 
2.5 进度组件
 
1. LinearProgressIndicator 线性进度
- 用途：批量生成任务、章节生成进度、资产渲染进度
- 支持确定进度（current/total）+文字计数展示
2. CircularProgressIndicator 环形加载
- 小尺寸20dp：AI思考中、单行加载
- 大尺寸48dp：全屏页面初始化加载
 
2.6 分割线 Divider
 
水平分割：厚度1dp，颜色outlineVariant，margin上下sm=8dp
垂直分割：用于按钮组、Tab栏内分隔
 
三、分子/有机体复合业务组件（业务专用封装）
 
3.1 消息气泡 MessageBubble（AI对话模块）
 
完整状态扩展
 
1. 用户消息 UserBubble
- 背景：primaryContainer，文字onPrimary
- 圆角：topEnd small 4dp，其余large 16dp
2. AI消息 SystemBubble
- 背景：surfaceVariant，文字onSurface
- 左侧Bot头像32dp圆形
- 圆角：topStart small 4dp，其余large 16dp
3. 流式思考状态 ThinkingIndicator
- 左侧Bot图标+环形加载+文字“AI思考中...”
4. 错误消息 ErrorBubble
- 气泡底色errorContainer，下方红色错误文字+重试按钮
5. 附属子组件（气泡内可嵌套）
- TextContent：纯文本对话
- ToolCallChip：技能调用标签（如【提取剧情事件】）
- SuggestionButtons：快捷推荐按钮（生成下一章、优化剧本）
- ImagePreview：AI生成图片缩略图卡片
 
3.2 聊天输入框 ChatInputArea
 
固定高度64dp，底部悬浮卡片
结构：附件IconButton + 多行输入框（weight1）+ 发送/停止按钮
 
- 流式生成中：替换为停止按钮（error色）
- 无文字时发送按钮置灰禁用
 
3.3 项目卡片 ProjectCard
 
高度自适应，最小120dp
结构分层：
 
1. 标题 TitleLarge 项目名称
2. 描述 BodyMedium 项目简介（空则隐藏）
3. 标签行：题材Chip + 画风Chip
4. 底部LabelSmall 创建时间
交互：全局点击跳转项目详情；左滑出现删除按钮
 
3.4 功能卡片 FunctionCard
 
单行水平布局，高度64dp
左侧大图标HeadlineMedium，右侧双行文字（TitleMedium标题+BodySmall描述）
全局点击跳转对应模块（小说/剧本/分镜/资产库）
 
3.5 任务进度卡片 ProgressTaskCard
 
用于任务队列弹窗、页面任务列表
结构：
 
- 任务名称 TitleSmall
- 进度条 LinearProgressIndicator
- 计数文本 LabelMedium（当前/总数）
- 右侧状态Chip（运行中/成功/失败）
 
3.6 资产卡片 AssetCard（资产库专用）
 
网格布局 2列/3列自适应
内部：缩略图占位 + 资产名称LabelLarge + 底部业务标签（本集衍生/全局资产）
点击预览大图，长按弹出操作菜单（导出、删除、加入分镜）
 
3.7 章节条目 ChapterItem（小说/剧本列表）
 
单行列表项，左右分布
左侧章节名称 TitleSmall；右侧状态Chip（待提取/已提取/已生成剧本）
点击进入编辑/事件提取页面
 
3.8 模型配置块 ModelBlock（设置-AI模型页面）
 
边框outline medium圆角，内部分组：
模型类型下拉框、模型ID输入框、温度/最大长度双列输入、删除按钮
支持动态新增多个模型块
 
3.9 内部Tab栏 InnerTabBar（页面内分页切换）
 
横向滚动容器，背景surfaceVariant
Tab按钮：未激活文字onSurfaceVariant；激活primaryContainer+primary文字
单Tab最小宽度适配文字，支持横向滑动多标签
 
四、全局页面标准布局模板（7大核心页面完整布局）
 
4.1 页面通用基础布局模板
 
plaintext
  
┌─────────────────────────────┐
│ TopAppBar (56dp)            │
│ 标题 + 右侧操作图标         │
├─────────────────────────────┤
│ 页面内容区（可滚动）        │
│ 左右边距lg=16dp             │
│ 顶部margin lg=16dp          │
│ 底部预留80dp（导航栏）      │
│                             │
│ 卡片/列表/Tab组件           │
├─────────────────────────────┤
│ BottomNavigation (58dp)     │
│ 6个底部Tab：项目/小说/骨架/ │
│ 导演镜头/资产库/设置        │
└─────────────────────────────┘
 
 
底部导航规则：
 
- TabItem：图标+LabelMedium文字上下排列
- 激活态：primary文字+顶部2dp主色边线
- 未激活：onSurfaceVariant
 
4.2 页面1：项目列表页 project_list
 
1. TopAppBar：左上角菜单图标，中间Toonflow标题，右侧设置图标
2. 内容区：ProjectCard列表垂直排列，卡片间距sm=8dp
3. 空状态：居中大图标+提示文字+【新建项目】主按钮
4. 右下角悬浮FAB（56dp圆形）：新建项目
 
4.3 页面2：项目详情页 project_detail
 
1. TopAppBar：返回箭头、项目名称、删除图标
2. 顶部InfoCard：项目基础信息（题材、画风、视频比例、默认模型）
3. 分区标题HeadlineSmall：快捷功能
4. 垂直排列FunctionCard：AI对话、小说管理、剧本创作、分镜设计、资产库
5. 底部FAB：打开AI对话窗口
 
4.4 页面3：小说管理页 novel_list
 
1. TopAppBar：返回、小说管理标题、批量导入按钮
2. InnerTabBar 内部分页：AI生成小说 / 章节管理 / 剧情事件提取
3. Tab1 AI生成模块：双列参数输入框、文本提示输入区、生成按钮
4. Tab2 章节列表：ChapterItem条目，批量新建章节按钮
5. Tab3 事件提取：章节下拉选择、事件三色标签卡片、导出/生成剧本按钮
 
4.5 页面4：骨架改编&剧本页 script_list
 
1. 内部Tab：故事骨架、改编策略、剧本生成、本集资产
2. 骨架Tab：核心剧情输入、生成XML/AI校验双按钮
3. 剧本Tab：剧集下拉、剧本预览代码框、生成本集剧本按钮
4. 本集资产Tab：双栏卡片（角色/场景道具）、资产入库、跳转导演页按钮
 
4.6 页面5：导演分镜页 storyboard
 
1. 内部Tab：本集衍生资产、导演规划、分镜表、分镜成片导出
2. 衍生资产Tab：提示警告卡片、批量生成衍生图按钮
3. 分镜Tab：表格布局分镜单元格、单镜头时长限制提示
4. 成片Tab：导出图片/视频按钮组
 
4.7 页面6：全局资产库 asset_list
 
1. 内部Tab：全部角色、全部场景、全部道具、衍生资产汇总
2. 网格2列AssetCard布局
3. 卡片底部区分标签：assetTag全局资产 / episodeTag本集衍生
 
4.8 页面7：系统设置页 settings
 
1. 内部Tab：AI模型商、模型参数、Skills管理、提示词、调试日志
2. AI模型商Tab：新增厂商按钮、动态ModelBlock列表
3. 模型参数Tab：温度、上下文、图片尺寸等参数输入框
4. Skills管理：开关组件控制各AI技能启用/禁用
 
4.9 页面10：AI对话页 chat
 
1. TopAppBar：返回、当前Agent名称（小说/剧本/生产）
2. 滚动消息区：MessageBubble气泡垂直堆叠，自动滚动到底部
3. 底部固定ChatInputArea输入框
4. 弹出任务队列浮层：任务进度卡片列表
 
五、弹窗/浮层/模态组件规范
 
5.1 底部弹出抽屉 BottomSheet
 
1. 圆角顶部extraLarge=24dp，高度自适应，最大70%屏幕高度
2. 顶部拖动条（4dp宽，40dp长，surfaceVariant）
3. 适用场景：任务队列、剧集选择、资产筛选菜单
 
5.2 居中对话框 Dialog
 
1. 宽度最大440dp，圆角large=16dp，上下双按钮（取消/确认）
2. 适用场景：删除确认、新建项目弹窗、模型新增弹窗
 
5.3 全屏模态弹窗 FullScreenDialog
 
全屏覆盖，带顶部返回栏，用于大图预览、分镜全屏编辑
 
5.4 轻提示 SnackBar
 
底部短时弹出，支持单按钮操作
 
- 成功：success文字；失败：error文字
- 场景：保存成功、创建失败、导出完成
 
5.5 下拉菜单 DropdownMenu
 
附着点击按钮，高程6dp阴影，选项高度48dp，hover高亮primaryContainer
 
六、全业务交互&状态规范
 
6.1 加载状态规范
 
1. 页面初始化全屏加载：居中大环形进度+文字“加载中”
2. 局部加载（按钮/卡片）：按钮内小环形加载，文字隐藏
3. AI流式生成：气泡内实时逐字输出，底部显示停止按钮
 
6.2 空状态规范
 
统一空状态组件：大尺寸图标 + HeadlineSmall提示标题 + BodySmall说明文字 + 操作按钮
适用场景：无项目、无章节、无资产、无任务记录
 
6.3 错误状态规范
 
1. 网络错误：页面居中错误图标、提示文字、【重试】按钮
2. 操作失败：气泡ErrorBubble + SnackBar报错
3. 任务失败：任务卡片红色error标签，支持重试按钮
 
6.4 手势交互完整规范
 
手势 作用对象 交互行为 
单击 所有卡片、按钮、Tab、条目 跳转页面/切换Tab/执行操作 
长按 资产卡片、章节条目 弹出操作菜单（复制、删除、导出） 
左滑 项目卡片、任务条目 右侧滑出删除按钮 
横向滑动 内部Tab栏 切换分页 
纵向滑动 页面滚动区域 上下滚动内容 
下拉 列表顶部 刷新数据 
点击空白遮罩 弹窗/抽屉 关闭浮层 
 
6.5 任务状态UI映射（TaskRecord状态机）
 
任务状态 UI表现 标签色 
RUNNING 线性进度条+环形加载 tertiary（黄色） 
SUCCESS 进度100%+完成图标 success（绿色） 
FAILED 红色错误文字+重试按钮 error（红色） 
 
七、动效与过渡规范
 
7.1 页面路由过渡
 
1. 普通页面跳转：水平左右滑动（从右推入）
2. 返回上一页：水平向右滑出
3. 弹窗弹出：淡入+垂直上浮缩放动画
4. 抽屉弹出：底部向上滑入
 
7.2 组件内动效
 
1. Tab切换：内容淡入淡出，时长200ms
2. 按钮点击：轻微缩放按压反馈（scale 0.96）
3. 流式文字输出：逐字打字动画，间隔30ms
4. 进度条：平滑数值过渡动画
 
八、深色/浅色模式切换规范
 
1. 全局一键切换开关（设置页面），状态持久化DataStore
2. 所有组件色彩自动绑定M3 colorScheme，无需单独写两套样式
3. 图片/图标自适应亮度：深色模式图标提升亮度，避免过暗看不清
4. 分割线、阴影明暗自动适配，深色模式阴影透明度降低
 
九、无障碍设计规范
 
1. 所有Icon、图片添加contentDescription语义描述
2. 文字对比度满足WCAG AA标准：
- 正文文字：对比度≥4.5:1
- 大号标题：对比度≥3:1
3. 可点击组件最小触摸区域48×48dp
4. 支持系统字体缩放，所有sp单位自适应放大，不截断布局
5. 错误状态添加语音语义标记，屏幕阅读器可识别失败提示
 
十、业务模块UI差异化适配补充
 
10.1 Agent智能体系统UI区分
 
- Novel Agent：novelTag青色标识，卡片左侧4dp青色边线
- Script Agent：scriptTag紫色标识，卡片左侧4dp紫色边线
- Production Agent：episodeTag橙色标识，卡片左侧4dp橙色边线
对话顶部标题显示当前Agent类型，搭配对应颜色图标
 
10.2 Skill技能管理UI
 
技能列表FilterChip多选标签，按SkillCategory分类着色：
小说类青色、剧本类紫色、生产分镜橙色、角色资产绿色
 
10.3 数据库实体页面UI区分
 
1. Project项目：主色primary紫色
2. Novel小说：青色novelTag
3. Script剧本：紫色scriptTag
4. Storyboard分镜：橙色episodeTag
5. Asset资产：绿色assetTag
页面顶部卡片左侧增加对应颜色标识条，快速区分模块
 
10.4 AI模型提供商区分
 
不同AI厂商使用不同颜色Chip标记：
DeepSeek蓝色、AgnesAI紫色、OpenAI绿色、自定义厂商灰色
 
十一、落地开发约束（Compose开发强制规范）
 
1. 禁止硬编码色值、字号、间距、圆角，全部使用预设Token
2. 所有页面拆分可复用Composable组件，原子组件统一封装在 ui/component/ 目录
3. 页面布局与业务逻辑分离：UI仅负责展示，ViewModel处理数据
4. 所有滚动容器增加滚动条美化，适配深色模式
5. 弹窗、浮层统一封装BaseDialog、BaseBottomSheet基础组件，减少重复代码
6. 状态统一使用StateFlow监听，UI仅响应状态渲染，无业务逻辑
 
十二、文档交付物扩展清单
 
1. 色彩色值表（Figma样式库同步）
2. 组件库页面（原子/分子组件预览图）
3. 7大核心页面高保真布局标注
4. 交互原型流程图（页面跳转、弹窗触发、任务生成全流程）
5. 深色/浅色模式双版本对照截图规范
6. Compose组件代码模板（配套现有UI文档Kotlin示例扩展）


以下为预览html参考设计视觉图！
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8" />
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover"/>
<title>Toonflow 短剧工作台｜稳定预览版</title>
<script src="https://cdn.tailwindcss.com"></script>
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/font-awesome@4.7.0/css/font-awesome.min.css">
<script>
tailwind.config = {
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        primary: '#818cf8',
        novel: '#2dd4bf',
        script: '#a78bfa',
        video: '#fb923c',
        neutral-dark: '#0f172a',
        card-dark: '#1e293b',
        card-hover: '#334155',
        text-main: '#f1f5f9',
        text-sub: '#94a3b8',
        success: '#34d399',
        warn: '#fbbf24',
        danger: '#f87171',
        info: '#38bdf8',
        pink400: '#f472b6',
        indigo400: '#818cf8',
        emerald400: '#34d399',
        amber400: '#fbbf24',
        sky400: '#38bdf8',
        red400: '#f87171'
      },
      borderRadius: {
        xl: '16px',
        full: '9999px'
      }
    }
  }
}
</script>
<style>
* {
  box-sizing: border-box;
  -webkit-tap-highlight-color: transparent;
}
body {
  margin: 0;
  background: #0f172a;
  color: #f1f5f9;
  font-family: system-ui, sans-serif;
}
.app-wrap {
  max-width: 480px;
  margin: 0 auto;
  height: 100dvh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
}
.status-bar {
  height: 26px;
  background: #000000;
  font-size: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 14px;
  flex-shrink: 0;
}
.header-top {
  height: 56px;
  background: linear-gradient(135deg,#818cf8,#a78bfa);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 16px;
  flex-shrink: 0;
}
.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 16px;
}
.btn-header {
  background: rgba(255,255,255,0.18);
  padding: 6px 12px;
  border-radius: 10px;
  font-size: 12px;
  cursor: pointer;
}
.task-trigger {
  position: absolute;
  top: 92px;
  right: 14px;
  width: 44px;
  height: 44px;
  border-radius: 9999px;
  background: linear-gradient(135deg,#818cf8,#a78bfa);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 20px rgba(129,140,248,0.25);
  cursor: pointer;
  z-index: 5;
}
.task-modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.75);
  z-index: 9999;
  display: none;
  align-items: center;
  justify-content: center;
  padding: 16px;
}
.task-modal-mask.open { display: flex; }
.task-modal-box {
  width: 100%;
  max-width: 440px;
  height: 70dvh;
  background: #1e293b;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
}
.modal-head {
  padding: 14px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #334155;
}
.modal-content {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.task-card {
  padding: 12px;
  border-left: 4px solid #818cf8;
  background: rgba(129,140,248,0.06);
  border-radius: 12px;
  font-size: 12px;
}
.main-view {
  flex: 1;
  overflow-y: auto;
  padding-top: 64px;
  padding-left: 14px;
  padding-right: 14px;
  padding-bottom: calc(80px + env(safe-area-inset-bottom));
}
.page-block {
  display: none;
  flex-direction: column;
  gap: 16px;
}
.page-block.active { display: flex; }
.inner-tab-bar {
  display: flex;
  gap: 6px;
  background: #1e293b;
  padding: 8px;
  border-radius: 12px;
  overflow-x: auto;
}
.inner-tab {
  flex-shrink: 0;
  padding: 8px 14px;
  font-size: 12px;
  border-radius: 8px;
  cursor: pointer;
  color: #94a3b8;
  white-space: nowrap;
  user-select: none;
}
.inner-tab.active {
  background: linear-gradient(135deg,#818cf8,#a78bfa);
  color: white;
}
.inner-tab-content { display: none; }
.inner-tab-content.active { display: block; }
.card {
  background: #1e293b;
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 4px 16px rgba(0,0,0.25);
}
.card-head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 14px;
}
.list-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #334155;
  font-size: 13px;
}
.list-row:last-child { border-bottom: none; }
.badge {
  padding: 3px 10px;
  border-radius: 9999px;
  font-size: 11px;
}
.badge-success { background: rgba(52,211,153,0.12); color: #34d399; }
.badge-warn { background: rgba(251,191,36,0.12); color: #fbbf24; }
.badge-episode { background: rgba(167,139,250,0.15); color: #a78bfa; }
.badge-chapter { background: rgba(45,212,191,0.15); color: #2dd4bf; }
.asset-episode-tag { color:#fb923c; font-weight:600; }
.asset-all-tag { color:#34d399; font-weight:600; }
.btn {
  width: 100%;
  padding: 10px;
  border: none;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
}
.btn-gradient { background: linear-gradient(135deg,#818cf8,#a78bfa); color: white; }
.btn-green { background: #34d399; color: #0f172a; }
.btn-orange { background: #fb923c; color: white; }
.btn-dark { background: #334155; color: #f1f5f9; }
.btn-sm {
  width: auto;
  padding: 6px 12px;
  font-size: 12px;
  border-radius: 10px;
  cursor: pointer;
}
.input-box {
  width: 100%;
  background: transparent;
  border: 1px solid #475569;
  border-radius: 10px;
  padding: 10px 12px;
  color: #f1f5f9;
  font-size: 13px;
  outline: none;
}
.text-area-sm { min-height: 86px; resize: none; }
.text-area-md { min-height: 140px; resize: none; }
.code-preview {
  background: #0f172a;
  border: 1px solid #334155;
  border-radius: 10px;
  padding: 10px;
  font-family: monospace;
  font-size: 12px;
  color: #34d399;
  overflow-y: auto;
}
.grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.grid-3 { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 10px; }
.bottom-tab {
  height: 58px;
  background: #1e293b;
  border-top: 1px solid #334155;
  display: flex;
  flex-shrink: 0;
  padding-bottom: env(safe-area-inset-bottom);
}
.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-size: 10px;
  color: #94a3b8;
  cursor: pointer;
}
.tab-item.active {
  color: #818cf8;
  border-top: 2px solid #818cf8;
}
.tip-alert {
  padding: 10px;
  border-radius: 10px;
  background: rgba(251,191,36,0.08);
  border: 1px solid rgba(251,191,36,0.2);
  color: #fbbf24;
  font-size: 12px;
}
::-webkit-scrollbar { width: 5px; height: 5px; }
::-webkit-scrollbar-thumb { background: #475569; border-radius: 10px; }
.model-block {
  border: 1px solid #475569;
  border-radius: 10px;
  padding: 12px;
  margin-bottom: 10px;
}
.model-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
</style>
</head>
<body>
<div class="app-wrap">
  <div class="status-bar">
    <span>10:00</span>
    <span>Toonflow 短剧工作台</span>
    <span>5G <i class="fa fa-battery-full"></i></span>
  </div>
  <header class="header-top">
    <div class="header-title">
      <i class="fa fa-film text-lg"></i>
      <span>Toonflow 短剧工作台</span>
    </div>
    <div class="btn-header" onclick="switchMainPage('setting')">系统设置</div>
  </header>

  <div class="task-trigger" onclick="openTaskModal()">
    <i class="fa fa-tasks text-lg"></i>
  </div>

  <div id="taskMask" class="task-modal-mask" onclick="closeTaskModal()">
    <div class="task-modal-box" onclick="event.stopPropagation()">
      <div class="modal-head">
        <span class="font-semibold">任务队列｜本集资产查看</span>
        <i class="fa fa-times cursor-pointer" onclick="closeTaskModal()"></i>
      </div>
      <div class="modal-content">
        <label class="text-xs text-sub">选择剧集查看对应资产</label>
        <select class="input-box text-xs mb-3">
          <option>第1集：小镇初遇</option>
          <option>第2集：宗门追兵</option>
          <option>第3集：秘境开启</option>
        </select>
        <div class="tip-alert">下方为当前选中剧集提取资产</div>
        <div class="task-card">
          <div class="font-semibold asset-episode-tag">第1集｜本集资产</div>
          <div class="text-sub mt-1">角色：凌玄、苏晚、地痞头目</div>
          <div class="text-sub">场景：青云酒楼</div>
          <div class="text-sub">道具：上古玉佩</div>
        </div>
        <div class="task-card">
          <div class="font-semibold text-indigo400">资产渲染 task_001（处理中 6/12）</div>
        </div>
        <div class="task-card">
          <div class="font-semibold text-emerald400">1-3集剧本（已完成）</div>
        </div>
      </div>
    </div>
  </div>

  <main class="main-view">
    <!--项目页-->
    <div id="page-project" class="page-block">
      <div class="card">
        <div class="card-head"><i class="fa fa-folder-open text-primary"></i>项目管理</div>
        <div class="grid-2 mb-3">
          <button class="btn btn-gradient">新建短剧项目</button>
          <button class="btn btn-dark">导入本地项目包</button>
        </div>
        <label class="text-xs text-sub mb-2 block">我的项目列表</label>
        <div class="list-row">
          <div>
            <div class="font-semibold">仙侠寻缘01</div>
            <div class="text-xs text-sub">10集 / 8章节</div>
          </div>
          <span class="badge badge-success">当前项目</span>
        </div>
        <div class="grid-2 gap-2 mt-4">
          <button class="btn btn-sm btn-dark">导出项目</button>
          <button class="btn btn-sm btn-danger">删除项目</button>
        </div>
      </div>
    </div>

    <!--小说页-->
    <div id="page-novel" class="page-block active">
      <div class="inner-tab-bar">
        <div class="inner-tab" onclick="switchInnerTab('novel','tab2')">AI小说生成</div>
        <div class="inner-tab" onclick="switchInnerTab('novel','tab1')">章节管理</div>
        <div class="inner-tab" onclick="switchInnerTab('novel','tab3')">事件提取【按章节】</div>
      </div>
      <div id="novel-tab2" class="inner-tab-content active card">
        <div class="card-head"><i class="fa fa-magic text-info"></i>AI小说生成</div>
        <div class="grid-2 mb-3">
          <div><label class="text-xs text-sub">题材</label><select class="input-box text-xs mt-1"><option>仙侠玄幻</option></select></div>
          <div><label class="text-xs text-sub">单章字数</label><input class="input-box text-xs mt-1" value="1200"></div>
        </div>
        <textarea class="input-box text-area-sm mb-3" placeholder="男主凌玄，上古玉佩转世下山"></textarea>
        <div class="grid-2 gap-2 mb-3">
          <button class="btn btn-sm btn-dark">保存模板</button>
          <button class="btn btn-gradient">流式生成章节</button>
        </div>
      </div>
      <div id="novel-tab1" class="inner-tab-content card">
        <div class="card-head"><i class="fa fa-book text-novel">章节管理</i></div>
        <div class="grid-2 mb-3">
          <button class="btn btn-sm btn-dark">批量导入TXT</button>
          <button class="btn btn-gradient">新建章节</button>
        </div>
        <div class="list-row"><span>第1章 楔子</span><span class="badge badge-chapter">已提取事件</span></div>
        <div class="list-row"><span>第2章 酒楼冲突</span><span class="badge badge-chapter">已提取事件</span></div>
        <div class="list-row"><span>第3章 秘境开启</span><span class="badge badge-warn">待提取</span></div>
      </div>
      <div id="novel-tab3" class="inner-tab-content card">
        <div class="card-head"><i class="fa fa-list-alt text-success">剧情事件（按章节分组）</i></div>
        <label class="text-xs text-sub">选择章节查看对应事件</label>
        <select class="input-box text-xs mb-3">
          <option>第1章 楔子</option>
          <option>第2章 酒楼冲突</option>
          <option>第3章 秘境开启</option>
        </select>
        <div class="grid-3 mb-3">
          <div class="border border-emerald400/30 p-2 rounded-lg text-xs">
            <div class="text-emerald400 font-semibold">背景事件</div>
            <p class="mt-1 text-sub">凌玄下山寻玉佩</p>
          </div>
          <div class="border border-amber400/30 p-2 rounded-lg text-xs">
            <div class="text-amber400 font-semibold">冲突事件</div>
            <p class="mt-1 text-sub">地痞勒索老板娘</p>
          </div>
          <div class="border border-sky400/30 p-2 rounded text-xs">
            <div class="text-sky400 font-semibold">高潮事件</div>
            <p class="mt-1 text-sub">玉佩力量觉醒</p>
          </div>
        </div>
        <div class="code-preview text-area-md mb-3">
{"chapterId":"chap_01","events":[{"type":"conflict","角色":["凌玄","苏晚"]}]}
        </div>
        <div class="grid-2 gap-2">
          <button class="btn btn-green">导出本章事件</button>
          <button class="btn btn-gradient">导入骨架生成剧集</button>
        </div>
      </div>
    </div>

    <!--骨架改编-->
    <div id="page-skeleton" class="page-block">
      <div class="inner-tab-bar">
        <div class="inner-tab" onclick="switchInnerTab('skeleton','tab1')">故事骨架</div>
        <div class="inner-tab" onclick="switchInnerTab('skeleton','tab2')">改编策略</div>
        <div class="inner-tab" onclick="switchInnerTab('skeleton','tab3')">剧本生成【按集】</div>
        <div class="inner-tab" onclick="switchInnerTab('skeleton','tab4')">剧本资产【本集】</div>
      </div>
      <div id="skeleton-tab1" class="inner-tab-content active card">
        <div class="card-head"><i class="fa fa-sitemap text-script">故事骨架</i></div>
        <div class="border border-indigo400/30 p-3 rounded-lg mb-3">
          <label class="text-xs font-semibold">核心剧情</label>
          <input class="input-box text-xs mt-1" value="落魄仙尊寻玉佩复仇">
        </div>
        <div class="grid-2 gap-2">
          <button class="btn btn-sm btn-gradient">生成骨架XML</button>
          <button class="btn btn-sm btn-dark">AI校验</button>
        </div>
      </div>
      <div id="skeleton-tab2" class="inner-tab-content card">
        <div class="card-head"><i class="fa fa-file-text text-pink400">短剧改编规则</i></div>
        <textarea class="input-box text-area-md mb-3" placeholder="每集前置冲突，结尾付费反转"></textarea>
        <button class="btn btn-gradient">保存策略生成剧本</button>
      </div>
      <div id="skeleton-tab3" class="inner-tab-content card">
        <div class="card-head"><i class="fa fa-file-code text-script">剧本生成（单集独立）</i></div>
        <label class="text-xs text-sub">切换剧集预览</label>
        <select class="input-box text-xs mb-3">
          <option>第1集：小镇初遇</option>
          <option>第2集：宗门追兵</option>
          <option>第3集：秘境开启</option>
        </select>
        <div class="grid-2 text-xs mb-3">
          <div>单集时长：5分钟竖屏</div>
          <div>美术：2D国风</div>
        </div>
        <button class="btn btn-gradient mb-3">生成当前集剧本</button>
        <div class="code-preview text-area-md">&lt;scriptItem name="第1集"&gt;苏晚：小店交不起保护费...</div>
      </div>
      <div id="skeleton-tab4" class="inner-tab-content card">
        <div class="card-head"><i class="fa fa-cubes text-success">本集剧本资产</i></div>
        <label class="text-xs text-sub">选择剧集提取资产</label>
        <select class="input-box text-xs mb-3">
          <option>第1集：小镇初遇</option>
          <option>第2集：宗门追兵</option>
        </select>
        <div class="grid-2 gap-3 mb-3">
          <div class="border p-2 text-xs">
            <div class="asset-episode-tag">本集角色</div>
            <div class="border-b py-1">凌玄</div>
            <div class="border-b py-1">苏晚</div>
          </div>
          <div class="border p-2 text-xs">
            <div class="asset-episode-tag">本集场景/道具</div>
            <div class="border-b py-1">青云酒楼</div>
            <div>上古玉佩</div>
          </div>
        </div>
        <div class="tip-alert">提取后自动存入项目全局资产库</div>
        <div class="space-y-2">
          <button class="btn btn-green">本集资产入库</button>
          <button class="btn btn-orange" onclick="switchMainPage('camera')">进入导演生成本集衍生</button>
        </div>
      </div>
    </div>

    <!--导演镜头-->
    <div id="page-camera" class="page-block">
      <div class="inner-tab-bar">
        <div class="inner-tab" onclick="switchInnerTab('camera','tab1')">衍生资产【本集】</div>
        <div class="inner-tab" onclick="switchInnerTab('camera','tab2')">导演规划</div>
        <div class="inner-tab" onclick="switchInnerTab('camera','tab3')">分镜表</div>
        <div class="inner-tab" onclick="switchInnerTab('camera','tab4')">分镜成片</div>
      </div>
      <div id="camera-tab1" class="inner-tab-content active card">
        <div class="card-head"><i class="fa fa-clone text-video">本集衍生资产</i></div>
        <div class="tip-alert">仅基于当前剧集素材生成，统一标记【本集衍生】</div>
        <div class="text-xs text-sub mb-3">
          <span class="asset-episode-tag">本集衍生：</span>凌玄战斗黑衣、酒楼夜景
        </div>
        <button class="btn btn-orange">批量生成本集衍生图</button>
      </div>
      <div id="camera-tab2" class="inner-tab-content card">
        <div class="card-head"><i class="fa fa-video-camera text-info">单集导演规划</i></div>
        <div class="border p-3 text-xs mb-3">第1集酒楼对峙｜冲突等级7</div>
      </div>
      <div id="camera-tab3" class="inner-tab-content card">
        <div class="card-head"><i class="fa fa-table text-script">分镜编辑器</i></div>
        <div class="tip-alert">单镜头最长15秒</div>
      </div>
      <div id="camera-tab4" class="inner-tab-content card">
        <div class="card-head"><i class="fa fa-play-circle text-success">分镜&成片导出</i></div>
      </div>
    </div>

    <!--资产库-->
    <div id="page-assetLib" class="page-block">
      <div class="inner-tab-bar">
        <div class="inner-tab" onclick="switchInnerTab('assetLib','tab1')">全部角色</div>
        <div class="inner-tab" onclick="switchInnerTab('assetLib','tab2')">全部场景</div>
        <div class="inner-tab" onclick="switchInnerTab('assetLib','tab3')">全部道具</div>
        <div class="inner-tab" onclick="switchInnerTab('assetLib','tab4')">全部衍生资产</div>
      </div>
      <div id="assetLib-tab1" class="inner-tab-content active card">
        <div class="card-head"><i class="fa fa-user text-novel">项目全部角色资产</i></div>
        <div class="grid-2 gap-2 mb-3">
          <div class="border rounded p-2 h-20 flex flex-col items-center justify-center text-xs">
            凌玄 <span class="asset-all-tag">全局资产</span>
          </div>
          <div class="border rounded p-2 h-20 flex flex-col items-center text-xs">苏晚</div>
        </div>
      </div>
      <div id="assetLib-tab2" class="inner-tab-content card">
        <div class="card-head"><i class="fa fa-building text-sky400">项目全部场景</i></div>
      </div>
      <div id="assetLib-tab3" class="inner-tab-content card">
        <div class="card-head"><i class="fa fa-gem text-amber400">项目全部道具</i></div>
      </div>
      <div id="assetLib-tab4" class="inner-tab-content card">
        <div class="card-head"><i class="fa fa-clone text-pink400">全项目衍生汇总</div>
        <div class="list-row text-xs">
          <span>【第1集衍生】凌玄战斗黑衣</span>
          <span class="badge badge-success">已渲染</span>
        </div>
      </div>
    </div>

    <!--设置页面-->
    <div id="page-setting" class="page-block">
      <div class="inner-tab-bar">
        <div class="inner-tab" onclick="switchInnerTab('setting','tab0')">AI模型商</div>
        <div class="inner-tab" onclick="switchInnerTab('setting','tab1')">AI模型配置</div>
        <div class="inner-tab" onclick="switchInnerTab('setting','tab2')">Skills管理</div>
        <div class="inner-tab" onclick="switchInnerTab('setting','tab3')">提示词管理</div>
        <div class="inner-tab" onclick="switchInnerTab('setting','tab4')">调试</div>
      </div>
      <div id="setting-tab0" class="inner-tab-content active card">
        <div class="card-head"><i class="fa fa-server text-sky400">AI模型供应商</i></div>
        <div class="grid-2 mb-3">
          <button class="btn btn-gradient" onclick="addModelBlock()">新增厂商</button>
          <button class="btn btn-dark">刷新列表</button>
        </div>
        <div id="modelWrap"></div>
      </div>
      <div id="setting-tab1" class="inner-tab-content card">
        <div class="card-head"><i class="fa fa-sliders text-indigo400">模型参数配置</i></div>
        <p class="text-xs text-sub">选择上方模型后配置temperature、上下文长度等参数</p>
      </div>
      <div id="setting-tab2" class="inner-tab-content card">
        <div class="card-head"><i class="fa fa-wrench text-amber400">Skills功能管理</i></div>
        <p class="text-xs text-sub">启用/禁用：小说生成、剧集拆分、资产提取、分镜生成技能</p>
      </div>
      <div id="setting-tab3" class="inner-tab-content card">
        <div class="card-head"><i class="fa fa-comment text-emerald400">全局提示词管理</i></div>
        <p class="text-xs text-sub">剧本Prompt、角色描述模板、图片生成提示词统一管理</p>
      </div>
      <div id="setting-tab4" class="inner-tab-content card">
        <div class="card-head"><i class="fa fa-bug text-red400">调试日志</i></div>
        <p class="text-xs text-sub">API请求日志、AI返回原始数据查看</p>
      </div>
    </div>
  </main>

  <!--底部主导航栏-->
  <nav class="bottom-tab">
    <div class="tab-item" onclick="switchMainPage('project')"><i class="fa fa-folder"></i><span>项目</span></div>
    <div class="tab-item active" onclick="switchMainPage('novel')"><i class="fa fa-book"></i><span>小说</span></div>
    <div class="tab-item" onclick="switchMainPage('skeleton')"><i class="fa fa-sitemap"></i><span>骨架改编</span></div>
    <div class="tab-item" onclick="switchMainPage('camera')"><i class="fa fa-video-camera"></i><span>导演镜头</span></div>
    <div class="tab-item" onclick="switchMainPage('assetLib')"><i class="fa fa-th-large"></i><span>总资产库</span></div>
    <div class="tab-item" onclick="switchMainPage('setting')"><i class="fa fa-cogs"></i><span>设置</span></div>
  </nav>
</div>

<script>
// ========== 主页面切换函数（底部Tab）==========
function switchMainPage(pageName){
  // 隐藏所有页面
  document.querySelectorAll('.page-block').forEach(p=>p.classList.remove('active'));
  // 激活目标页面
  document.getElementById('page-' + pageName).classList.add('active');
  // 更新底部tab激活状态
  document.querySelectorAll('.tab-item').forEach(t=>t.classList.remove('active'));
  event.currentTarget.classList.add('active');
}

// ========== 页面内部子Tab切换函数 ==========
function switchInnerTab(parent, tab){
  //清除同页面所有子tab内容
  document.querySelectorAll(`#page-${parent} .inner-tab-content`).forEach(el=>el.classList.remove('active'));
  document.getElementById(`${parent}-${tab}`).classList.add('active');
  //清除同页面tab按钮激活
  document.querySelectorAll(`#page-${parent} .inner-tab`).forEach(el=>el.classList.remove('active'));
  event.currentTarget.classList.add('active');
}

//弹窗
function openTaskModal(){
  document.getElementById("taskMask").classList.add("open");
}
function closeTaskModal(){
  document.getElementById("taskMask").classList.remove("open");
}

let modelIndex = 1;
function addModelBlock(){
  const wrap = document.getElementById('modelWrap');
  const html = `
  <div class="model-block">
    <div class="model-title">
      <span class="text-xs font-semibold">模型 #${modelIndex}</span>
      <button class="btn btn-sm btn-danger" onclick="this.parentElement.parentElement.remove()">删除本条</button>
    </div>
    <label class="text-xs text-sub">模型类型</label>
    <select class="input-box text-xs mt-1 mb-2">
      <option>文本大模型</option>
      <option>图片生成模型</option>
      <option>视频生成模型</option>
    </select>
    <label class="text-xs text-sub">模型ID</label>
    <input class="input-box text-xs mt-1" placeholder="deepseek-v3">
    <div class="grid-2 gap-2">
      <div><label class="text-xs">温度</label><input class="input-box" value="0.7"></div>
      <div><label class="text-xs">最大长度</label><input class="input-box" value="4096"></div>
    </div>
  </div>`;
  wrap.insertAdjacentHTML('beforeend', html);
  modelIndex++;
}

window.onload = function(){
  const wrap = document.getElementById('modelWrap');
  wrap.innerHTML = `
  <div class="model-block">
    <div class="model-title">
      <span class="text-xs font-semibold">模型 #1</span>
      <button class="btn btn-sm btn-danger" onclick="this.parentElement.parentElement.remove()">删除本条</button>
    </div>
    <label class="text-xs text-sub">模型类型</label>
    <select class="input-box text-xs mt-1 mb-2">
      <option>文本大模型</option>
      <option>图片生成模型</option>
      <option>视频生成模型</option>
    </select>
    <label class="text-xs text-sub">模型ID</label>
    <input class="input-box text-xs mt-1" placeholder="deepseek-v3">
    <div class="grid-2 gap-2">
      <div><label class="text-xs">温度</label><input class="input-box" value="0.7"></div>
      <div><label class="text-xs">最大长度</label><input class="input-box" value="4096"></div>
    </div>
  </div>`;
}
</script>
</body>
</html>
