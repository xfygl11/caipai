# 个人助手 TV - 纯原生 Android 详细方案

## 一、项目概述

将现有 WebView 混合架构的影视+彩票 App 改造为**纯原生 Android 项目**，参考 FongMi/TVBox 系列 App 的 UI 和功能设计。

**核心架构**：
- 底部导航栏 5 Tab：[影视] [直播] [彩票] [历史/收藏] [设置]
- 影视模块：纯原生 Java + Media3 ExoPlayer + OkHttp + Glide
- 彩票模块：WebView 承载 nc-lottery.js（1319行完整保留）
- **智能链接识别**：仓库中所有链接自动识别类型（影视站点TVBox API / 直播源M3U），无需手动选择

---

## 二、项目结构

```
com.personalassistant.app/
├── MainActivity.java                    # 主Activity (底部导航 + Fragment容器)
├── ui/
│   ├── MovieHomeFragment.java           # 影视首页 (分类Tab + 影片网格)
│   ├── LiveHomeFragment.java            # 直播首页 (分类Tab + 频道列表)
│   ├── HistoryFragment.java             # 播放历史页
│   ├── FavoriteFragment.java            # 收藏页
│   ├── SettingsFragment.java            # 设置页
│   ├── MovieSearchActivity.java         # 搜索页
│   ├── MovieDetailActivity.java         # 影片详情页
│   ├── MoviePlayerActivity.java         # ExoPlayer 播放器
│   ├── LivePlayerActivity.java          # 直播播放器
│   ├── RepoManageActivity.java          # 仓库管理页
│   ├── SiteSelectorDialog.java          # 站点选择弹窗
│   ├── LotteryFragment.java             # 彩票 WebView Fragment
│   └── MovieGridAdapter.java            # 影片网格 RecyclerView Adapter
├── movie/
│   ├── MovieApi.java                    # TVBox API 封装 (OkHttp + Gson)
│   ├── RepoManager.java                 # 仓库管理 (加载/解析/缓存)
│   ├── MovieParser.java                 # 播放链接解析 (vod_play_url 转 Episode)
│   └── LinkTypeDetector.java            # 智能链接识别 (MV/LIVE/自动)
├── live/
│   ├── LiveApi.java                     # 直播源 API 封装
│   └── LiveParser.java                  # M3U 直播源解析
├── bridge/
│   └── AndroidJSBridge.java             # WebView JS 桥接
├── db/
│   ├── entity/
│   │   ├── MovieEntity.java             # 影片实体
│   │   ├── HistoryEntity.java           # 播放历史实体
│   │   └── FavoriteEntity.java          # 收藏实体
│   ├── dao/
│   │   ├── MovieDao.java                # 影片 DAO
│   │   ├── HistoryDao.java              # 历史 DAO
│   │   └── FavoriteDao.java             # 收藏 DAO
│   └── AppDatabase.java                 # Room 数据库
└── util/
    ├── HttpUtil.java                    # HTTP 工具类 (OkHttp 封装)
    └── PrefUtil.java                    # SharedPreferences 工具类
```

### 资源文件

```
res/
├── layout/
│   ├── activity_main.xml                # 主布局 (FragmentContainer + BottomNav)
│   ├── fragment_movie_home.xml          # 影视首页
│   ├── fragment_live_home.xml           # 直播首页
│   ├── fragment_history.xml             # 播放历史
│   ├── fragment_favorite.xml            # 收藏
│   ├── fragment_settings.xml            # 设置页
│   ├── activity_movie_search.xml        # 搜索页
│   ├── activity_movie_detail.xml        # 影片详情
│   ├── activity_movie_player.xml        # 播放器
│   ├── activity_live_player.xml         # 直播播放器
│   ├── activity_repo_manage.xml         # 仓库管理
│   ├── dialog_site_selector.xml         # 站点选择弹窗
│   ├── item_movie_grid.xml              # 影片卡片
│   ├── item_live_channel.xml            # 直播频道卡片
│   ├── item_episode_button.xml          # 剧集按钮
│   ├── item_category_tab.xml            # 分类Tab
│   ├── item_history_row.xml             # 历史记录行
│   ├── item_favorite_row.xml            # 收藏行
│   └── layout_skeleton.xml              # 骨架屏
├── menu/
│   └── bottom_nav_menu.xml              # 底部导航菜单
├── values/
│   ├── colors.xml                       # 配色方案
│   ├── dimens.xml                       # 尺寸规范
│   └── styles.xml                       # 样式
└── drawable/
    ├── bg_movie_card.xml                # 影片卡片背景
    ├── bg_live_card.xml                 # 直播卡片背景
    ├── bg_tab_selected.xml              # Tab选中背景
    └── ic_player_controls.xml           # 播放器图标
```

---

## 三、实现功能

### 3.1 影视模块功能

| 功能 | 说明 | 优先级 |
|------|------|--------|
| 首页分类Tab | 水平滚动，当前选中紫色高亮+底部边框 | P0 |
| 影片网格 | 3列GridLayoutManager，Glide加载海报 | P0 |
| 下拉加载更多 | 滚动到底部自动加载下一页 | P0 |
| 搜索 | 独立搜索页，支持精确/模糊搜索 | P1 |
| 影片详情 | 海报+标题+评分+导演+主演+简介+剧集列表 | P0 |
| ExoPlayer播放 | Media3 ExoPlayer，硬解码+SurfaceView | P0 |
| 剧集切换 | 底部水平滚动按钮，点击切换 | P0 |
| 自动播放 | 当前集播完自动下一集 | P1 |
| 仓库管理 | 输入仓库地址→解析站点→选择站点 | P1 |
| 站点切换 | 长按"影视"Tab 600ms打开仓库管理 | P1 |
| 上次站点恢复 | 从SharedPreferences恢复上次使用的站点 | P1 |
| 骨架屏 | 加载状态显示骨架屏动画 | P2 |
| 空状态 | 无数据时显示提示 | P2 |

### 3.2 直播模块功能

| 功能 | 说明 | 优先级 |
|------|------|--------|
| 智能链接识别 | 仓库链接自动识别影视站点(MV)或直播源(LIVE) | P0 |
| 直播分类Tab | 水平滚动，当前选中紫色高亮+底部边框 | P0 |
| 频道列表 | 3列GridLayoutManager，Glide加载频道logo | P0 |
| 直播播放 | ExoPlayer播放m3u8直播流 | P0 |
| 频道切换 | 底部水平滚动按钮，点击切换 | P0 |
| M3U解析 | 解析M3U直播源格式 | P0 |
| 频道收藏 | 收藏常用频道 | P1 |
| 频道分组 | 按分类（央视/卫视/地方）分组 | P2 |

### 3.3 历史记录功能

| 功能 | 说明 | 优先级 |
|------|------|--------|
| 播放历史 | 记录观看过的影片和直播 | P0 |
| 历史记录列表 | 按时间倒序显示 | P0 |
| 清除历史 | 一键清除所有历史记录 | P1 |
| 删除单条 | 长按删除单条记录 | P1 |

### 3.4 收藏功能

| 功能 | 说明 | 优先级 |
|------|------|--------|
| 影片收藏 | 收藏喜欢的影片 | P0 |
| 收藏列表 | 显示所有收藏的影片 | P0 |
| 取消收藏 | 长按取消收藏 | P1 |
| 直播频道收藏 | 收藏常用直播频道 | P1 |

### 3.5 设置功能

| 功能 | 说明 | 优先级 |
|------|------|--------|
| 播放器设置 | ExoPlayer/硬解码/SurfaceView/缓冲/缓存 | P0 |
| 仓库管理 | 输入仓库地址→解析站点→选择站点 | P1 |
| 直播源设置 | 仓库自动导入 + 手动添加/删除/切换直播源 | P1 |
| 搜索设置 | 精确/模糊搜索模式 | P2 |
| 个性化设置 | PiP/弹幕/去广告/接口缓存/缩略图海报/全局无痕 | P2 |

### 3.6 彩票模块功能

| 功能 | 说明 | 优先级 |
|------|------|--------|
| WebView加载 | 承载 lottery.html | P0 |
| 8种彩票 | 大乐透/双色球/七乐彩/福彩3D/排列3/排列5/7星彩/快乐8 | P0 |
| 预测引擎 | 5种算法预测 | P0 |
| 历史数据 | 内置最近50期 + 联网同步 | P0 |
| 手动录入 | 手动输入开奖号码 | P0 |
| 奖级判断 | 自动判定中奖情况 | P0 |
| 倒计时 | 下期开奖倒计时 | P0 |

### 3.7 用户偏好设置 (SharedPreferences)

| Key | 类型 | 默认值 | 说明 |
|-----|------|--------|------|
| `current_site_name` | String | "" | 当前站点名称 |
| `current_site_api` | String | "" | 当前站点API地址 |
| `player_type` | int | 2 | 播放器: 0=系统, 2=Exo |
| `player_render` | int | 1 | 渲染: 0=TextureView, 1=SurfaceView |
| `decoder_mode` | String | "硬解码" | 解码方式 |
| `exo_buffer` | int | 150 | EXO缓冲秒数 |
| `exo_cache` | boolean | true | EXO磁盘缓存 |
| `search_mode` | String | "精确搜索" | 搜索模式 |
| `pip_enabled` | boolean | false | 画中画 |
| `danmaku_enabled` | boolean | false | 弹幕 |
| `ad_filter` | boolean | true | 去广告 |
| `api_cache` | boolean | true | 接口缓存 |
| `incognito` | boolean | false | 全局无痕 |
| `live_source_url` | String | "" | 当前直播源地址 |
| `custom_live_sources` | String | "[]" | 自定义直播源列表(JSON) |
| `favorite_channels` | String | "" | 收藏的直播频道(JSON) |

---

## 四、页面布局

### 4.1 主布局 (activity_main.xml)

```
┌─────────────────────────────────────┐
│                                     │
│           [Fragment容器]            │
│           (影视/直播/彩票/历史/设置) │
│                                     │
│                                     │
│                                     │
├─────────────────────────────────────┤
│  ◉影视    直播    彩票    历史/收藏  设置  │  ← BottomNavigationView
└─────────────────────────────────────┘
```

- **FragmentContainerView**：占据大部分屏幕，切换Fragment
- **BottomNavigationView**：固定在底部，5个Tab（影视/直播/彩票/历史/收藏/设置）
- **长按"影视"Tab 600ms**：打开 RepoManageActivity（仓库管理）

### 4.2 影视首页 (fragment_movie_home.xml)

```
┌─────────────────────────────────────┐
│ 🔍 搜索影视...         [搜索]       │  ← SearchBar (EditText + Button)
├─────────────────────────────────────┤
│ [推荐] [电影片] [连续剧] [综艺片]    │  ← HorizontalScrollView + Buttons
│ [动漫] [纪录片] [短剧] [体育]       │
├─────────────────────────────────────┤
│                                     │
│  ┌──────┐  ┌──────┐  ┌──────┐     │
│  │海报  │  │海报  │  │海报  │     │  ← RecyclerView Grid(3列)
│  │标题  │  │标题  │  │标题  │     │
│  │年份  │  │年份  │  │年份  │     │
│  └──────┘  └──────┘  └──────┘     │
│                                     │
│  ┌──────┐  ┌──────┐  ┌──────┐     │
│  │海报  │  │海报  │  │海报  │     │
│  │标题  │  │标题  │  │标题  │     │
│  │年份  │  │年份  │  │年份  │     │
│  └──────┘  └──────┘  └──────┘     │
│                                     │
├─────────────────────────────────────┤
│  ◉影视    直播    彩票    历史/收藏  设置  │
└─────────────────────────────────────┘
```

**布局组件**：
- **SearchBar**：LinearLayout (horizontal)，EditText + Button
- **分类Tab**：HorizontalScrollView 包裹 LinearLayout (horizontal)
- **影片网格**：RecyclerView + GridLayoutManager(spanCount=3)
- **下拉加载**：RecyclerView 滚动到底部自动触发

**影片卡片 (item_movie_grid.xml)**：
```
┌─────────────────────────────────────┐
│                                     │
│         [ImageView 海报]            │  ← Glide加载，宽高比 2:3
│                                     │
│  ┌─────────────────────────────────┐│
│  │ [TextView 标题]                 ││  ← 单行截断，白色
│  │ [TextView 年份/类型]            ││  ← 小字号，灰色
│  └─────────────────────────────────┘│
└─────────────────────────────────────┘
```

### 4.3 直播首页 (fragment_live_home.xml)

```
┌─────────────────────────────────────┐
│ [央视] [卫视] [地方] [港澳台]        │  ← HorizontalScrollView + Buttons
│ [教育] [记录] [其他]                │
├─────────────────────────────────────┤
│                                     │
│  ┌──────┐  ┌──────┐  ┌──────┐     │
│  │CCTV1 │ │CCTV2 │ │CCTV3 │     │  ← RecyclerView Grid(3列)
│  │logo  │  │logo  │  │logo  │     │
│  │央视1 │  │央视2 │  │央视3 │     │
│  └──────┘  └──────┘  └──────┘     │
│                                     │
│  ┌──────┐  ┌──────┐  ┌──────┐     │
│  │湖南卫视│ │浙江卫视│ │江苏卫视│     │
│  │logo  │  │logo  │  │logo  │     │
│  │湖南  │  │浙江  │  │江苏  │     │
│  └──────┘  └──────┘  └──────┘     │
│                                     │
├─────────────────────────────────────┤
│  ◉影视    直播    彩票    历史/收藏  设置  │
└─────────────────────────────────────┘
```

**直播源来源**：
- 仓库自动导入：从仓库JSON中识别 type=1 或 LIVE 类型的站点，自动添加到直播源列表
- 手动添加：用户在直播源设置中手动添加自定义直播源
- 切换直播源：直播首页顶部显示当前直播源名称，点击可切换
```

**布局组件**：
- **分类Tab**：HorizontalScrollView 包裹 LinearLayout (horizontal)
- **频道网格**：RecyclerView + GridLayoutManager(spanCount=3)
- **下拉加载**：RecyclerView 滚动到底部自动触发

**直播频道卡片 (item_live_channel.xml)**：
```
┌─────────────────────────────────────┐
│                                     │
│         [ImageView Logo]            │  ← Glide加载，宽高比 16:9
│                                     │
│  ┌─────────────────────────────────┐│
│  │ [TextView 频道名称]             ││  ← 居中，白色
│  └─────────────────────────────────┘│
└─────────────────────────────────────┘
```

### 4.4 搜索页 (activity_movie_search.xml)

```
┌─────────────────────────────────────┐
│              [← 返回]               │  ← Toolbar
├─────────────────────────────────────┤
│  ┌───────────────────────────────┐  │
│  │ 🔍 输入关键词搜索...          │  │  ← SearchBar (EditText)
│  └───────────────────────────────┘  │
├─────────────────────────────────────┤
│  搜索结果 (3列缩略图模式)           │
│  ┌──────┐  ┌──────┐  ┌──────┐     │
│  │海报  │  │海报  │  │海报  │     │  ← RecyclerView Grid(3列)
│  │标题  │  │标题  │  │标题  │     │
│  └──────┘  └──────┘  └──────┘     │
│                                     │
└─────────────────────────────────────┘
```

### 4.5 影片详情页 (activity_movie_detail.xml)

```
┌─────────────────────────────────────┐
│              [← 返回]               │  ← Toolbar
├─────────────────────────────────────┤
│  ┌───────────────────────────────┐  │
│  │                               │  │
│  │         [ImageView 海报]      │  │  ← Glide加载，圆角
│  │                               │  │
│  └───────────────────────────────┘  │
│                                     │
│  [TextView 标题]    ⭐ [评分]       │  ← 标题 + 评分
│  [类型] / [年份] / [地区]           │  ← 元信息行
│  导演：[TextView]                   │
│  主演：[TextView]                   │
│                                     │
│  ─────────────────────────────────  │
│  简介：                             │
│  [TextView 剧情简介，可折叠展开]     │
│                                     │
│  ─────────────────────────────────  │
│  剧集列表                           │
│  [第1集] [第2集] [第3集] ... [50集] │  ← HorizontalScrollView
│                                     │
└─────────────────────────────────────┘
```

**布局组件**：
- **海报**：CardView + ImageView (Glide加载，圆角14dp)
- **标题行**：LinearLayout (horizontal)，标题 + 评分
- **元信息**：TextView，斜杠分隔
- **导演/主演**：TextView，分开显示
- **简介**：CollapsibleTextView，默认显示3行，点击展开
- **剧集列表**：HorizontalScrollView + LinearLayout (horizontal)，每个按钮 item_episode_button.xml

### 4.6 播放器页 (activity_movie_player.xml)

```
┌─────────────────────────────────────┐
│                                     │
│                                     │
│           [ExoPlayer View]          │  ← SimpleExoPlayerView
│           (SurfaceView + 硬解码)     │
│                                     │
│                                     │
├─────────────────────────────────────┤
│  ⏮  ⏸  ⏭      00:00 / 45:00      │  ← 控制栏 (ExoPlayer自带)
│  ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬  │
│                                     │
│  [1] [2] [3] [4] [5] ... [50]      │  ← HorizontalScrollView 剧集切换
└─────────────────────────────────────┘
```

**布局组件**：
- **播放器**：SimpleExoPlayerView (Media3 UI)，隐藏控制器3秒后自动隐藏
- **剧集切换栏**：LinearLayout (horizontal) + HorizontalScrollView，位于播放器下方
- **全屏**：支持横竖屏切换

### 4.7 直播播放器页 (activity_live_player.xml)

```
┌─────────────────────────────────────┐
│                                     │
│                                     │
│           [ExoPlayer View]          │  ← SimpleExoPlayerView
│           (SurfaceView + 硬解码)     │
│                                     │
│                                     │
├─────────────────────────────────────┤
│  [← 返回]  [CCTV1 综合]     [收藏]  │  ← 顶部标题栏
│  ⏮  ⏸  ⏭      LIVE              │  ← 控制栏 (ExoPlayer自带)
│  ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬  │
│                                     │
│  [CCTV1] [CCTV2] [CCTV3] ... [湖南] │  ← HorizontalScrollView 频道切换
└─────────────────────────────────────┘
```

### 4.8 播放历史页 (fragment_history.xml)

```
┌─────────────────────────────────────┐
│  播放历史              [清除全部]   │  ← Toolbar
├─────────────────────────────────────┤
│  ┌─────────────────────────────────┐│
│  │ [ImageView 海报] [TextView 标题]│  │  ← item_history_row.xml
│  │ [TextView 时间]   [Button 删除]│  │
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────────┐│
│  │ [ImageView 海报] [TextView 标题]│  │
│  │ [TextView 时间]   [Button 删除]│  │
│  └─────────────────────────────────┘│
│                                     │
├─────────────────────────────────────┤
│  ◉影视    直播    彩票    历史/收藏  设置  │
└─────────────────────────────────────┘
```

**历史记录行 (item_history_row.xml)**：
```
┌─────────────────────────────────────┐
│ [ImageView 海报]                     │  ← 小尺寸海报
│ [TextView 标题]                     │  ← 标题
│ [TextView 播放时间]                  │  ← 时间
│ [Button 删除]                       │  ← 删除按钮
└─────────────────────────────────────┘
```

### 4.9 收藏页 (fragment_favorite.xml)

```
┌─────────────────────────────────────┐
│  我的收藏            [影视] [直播]  │  ← Toolbar + Tab切换
├─────────────────────────────────────┤
│  ┌──────┐  ┌──────┐  ┌──────┐     │
│  │海报  │  │海报  │  │海报  │     │  ← RecyclerView Grid(3列)
│  │标题  │  │标题  │  │标题  │     │
│  └──────┘  └──────┘  └──────┘     │
│                                     │
├─────────────────────────────────────┤
│  ◉影视    直播    彩票    历史/收藏  设置  │
└─────────────────────────────────────┘
```

### 4.10 设置页 (fragment_settings.xml)

```
┌─────────────────────────────────────┐
│              设  置                 │  ← Toolbar
├─────────────────────────────────────┤
│  ▶ 播放器设置                       │  ← CardView 设置项
│     ExoPlayer / 硬解码 / 缓冲 / 缓存 │
│                                     │
│  ▶ 仓库管理                         │  ← CardView 设置项
│     输入仓库地址 → 智能识别站点类型  │
│                                     │
│  ▶ 直播源设置                       │  ← CardView 设置项
│     仓库自动导入 + 手动添加/删除     │
│                                     │
│  ▶ 搜索设置                         │  ← CardView 设置项
│     精确搜索 / 模糊搜索              │
│                                     │
│  ▶ 个性化设置                       │  ← CardView 设置项
│     PiP / 弹幕 / 去广告 / 无痕模式   │
│                                     │
│  ▶ 关于                             │  ← CardView 设置项
│     版本信息 / 开源许可              │
├─────────────────────────────────────┤
│  ◉影视    直播    彩票    历史/收藏  设置  │
└─────────────────────────────────────┘
```

**设置项 (item_settings_card.xml)**：
```
┌─────────────────────────────────────┐
│  [TextView 标题]         [→ 箭头]   │
│  [TextView 副标题/当前值]            │
└─────────────────────────────────────┘
```

### 4.11 仓库管理页 (activity_repo_manage.xml)

```
┌─────────────────────────────────────┐
│              [← 返回]               │  ← Toolbar
├─────────────────────────────────────┤
│  仓库地址                           │
│  ┌───────────────────────────────┐  │
│  │ http://www.饭太硬.net/tv      │  │  ← EditText (输入仓库URL)
│  └───────────────────────────────┘  │
│           [加载仓库]                │  ← Button
├─────────────────────────────────────┤
│  已配置的站点 (共 N 个)             │
│  ┌───────────────────────────────┐  │
│  │ ● 非凡采集                      │  │  ← CardView 站点列表项
│  │   http://cj.ffzyapi.com/...    │  │    - 选中: 实心圆点+紫色
│  │   [切换] [删除]                │  │    - 未选中: 空心圆点+灰色
│  ├───────────────────────────────┤  │
│  │ ○ 暴风采集                      │  │
│  │   https://bfzyapi.com/...      │  │
│  │   [切换] [删除]                │  │
│  └───────────────────────────────┘  │
├─────────────────────────────────────┤
│           [ + 添加站点 ]            │  ← Button (底部)
└─────────────────────────────────────┘
```

**站点列表项 (item_site_row.xml)**：
```
┌─────────────────────────────────────┐
│  [RadioButton 选中状态] [TextView 名称]│
│  [TextView API地址 (灰色小字)]       │
│  [Button 切换]  [Button 删除]       │
└─────────────────────────────────────┘
```

### 4.12 站点选择弹窗 (dialog_site_selector.xml)

```
┌─────────────────────────────────────┐
│  选择影视源                         │  ← Dialog标题
├─────────────────────────────────────┤
│  ┌───────────────────────────────┐  │
│  │ 🎬 非凡采集                     │  │  ← CardView 站点卡片
│  │    cj.ffzyapi.com (MV)         │  │    🎬=影视  📺=直播
│  ├───────────────────────────────┤  │
│  │ 🎬 暴风采集                     │  │
│  │    bfzyapi.com (MV)            │  │
│  ├───────────────────────────────┤  │
│  │ 📺 直播源-央视                  │  │
│  │    live.example.com (LIVE)     │  │
│  ├───────────────────────────────┤  │
│  │ 📺 直播源-全频道                │  │
│  │    live.example.com/all.m3u    │  │
│  └───────────────────────────────┘  │
├─────────────────────────────────────┤
│           [ 取消 ]                  │  ← Button
└─────────────────────────────────────┘
```

**智能识别标识**：
- `🎬 MV`：影视站点，走TVBox API
- `📺 LIVE`：直播源，走M3U解析
- 无标识：自动请求URL后智能判断

---

## 五、仓库链接提取方法研究（基于公开仓库配置）

### 5.1 主流仓库 JSON 格式分析

通过分析多个公开仓库配置（高天流云 0821.json、YanG-1989 等），总结出以下仓库格式：

#### 格式一：YanG-1989 风格（推荐参考）

```json
{
  "spider": "./jar/fan.txt;md5;xxx",
  "logo": "https://...",
  "lives": [
    {
      "name": "初秋语•ipv4",
      "type": 0,
      "url": "./list.txt",
      "playerType": 2,
      "epg": "http://epg.112114.xyz/?ch={name}&date={date}",
      "logo": "https://live.fanmingming.com/tv/{name}.png"
    },
    {
      "name": "YanG•综合",
      "type": 0,
      "url": "https://tv.iill.top/m3u/Gather",
      "ua": "okhttp/3.15",
      "playerType": 2
    },
    {
      "name": "范明明•ipv6",
      "type": 0,
      "url": "https://live.fanmingming.com/tv/m3u/ipv6.m3u",
      "playerType": 2
    },
    {
      "name": "肥猫•综合",
      "type": 0,
      "url": "http://我不是.肥猫.live/TV/tvzb.txt",
      "playerType": 1
    }
  ],
  "sites": [
    {
      "key": "豆瓣",
      "name": "豆瓣┃搜索",
      "type": 3,
      "api": "csp_DouDou",
      "searchable": 0
    },
    {
      "key": "量子",
      "name": "量子┃采集",
      "type": 0,
      "api": "https://cj.lziapi.com/api.php/provide/vod/at/xml/",
      "searchable": 1,
      "changeable": 1
    },
    {
      "key": "非凡",
      "name": "非凡┃采集",
      "type": 0,
      "api": "http://cj.ffzyapi.com/api.php/provide/vod/at/xml/",
      "searchable": 1,
      "changeable": 1
    },
    {
      "key": "haiwaikan",
      "name": "海外看┃采集",
      "type": 1,
      "api": "https://haiwaikan.com/api.php/provide/vod",
      "searchable": 1,
      "changeable": 1
    },
    {
      "key": "暴風",
      "name": "暴風┃采集",
      "type": 1,
      "api": "https://bfzyapi.com/api.php/provide/vod",
      "searchable": 1,
      "changeable": 1
    }
  ]
}
```

#### 格式二：高天流云风格

```json
{
  "spider": "./jar/pg.jar;md5;xxx",
  "lives": [
    {
      "name": "YanG•直播",
      "type": 0,
      "url": "https://tv.iill.top/m3u/Live",
      "ua": "okhttp/3.15",
      "playerType": 2
    },
    {
      "name": "YanG•体育",
      "type": 0,
      "url": "https://tv.iill.top/m3u/Sport",
      "ua": "okhttp/3.15",
      "playerType": 2
    }
  ],
  "sites": [
    {
      "key": "百度",
      "name": "百度┃采集",
      "type": 1,
      "api": "https://api.apibdzy.com/api.php/provide/vod?ac=list",
      "searchable": 1,
      "categories": ["国产动漫","日韩动漫","大陆剧","欧美剧"]
    },
    {
      "key": "索尼",
      "name": "索尼┃采集",
      "type": 1,
      "api": "https://suoniapi.com/api.php/provide/vod",
      "searchable": 1,
      "changeable": 1
    }
  ]
}
```

### 5.2 站点类型与 type 字段含义

通过分析多个公开仓库，总结如下：

| type 值 | 含义 | api 特征 | 处理方式 |
|----------|------|----------|----------|
| `0` | XML 采集 | api 含 `/api.php/provide/vod/at/xml/` | 请求 ac=list 获取分类，ac=detail 获取影片 |
| `1` | JSON 采集 | api 含 `/api.php/provide/vod` 不含 xml | 请求 ac=list 获取分类，ac=detail 获取影片 |
| `2` | EXML 采集 | 较少见 | 同 type=0 |
| `3` | JAR/JS 插件 | api 以 `csp_` 开头或 `.js` 结尾 | 不走 TVBox API，跳过 |

**关键发现**：
- type=0 和 type=1 都是影视采集站点，区别在于返回格式（XML vs JSON）
- type=3 是插件类型（JAR/JS），不走标准 API
- 我们的 App 只支持 type=0 和 type=1 的影视采集站点

### 5.3 直播源提取方法

#### 方法一：从仓库 lives[] 数组提取

仓库 JSON 中的 `lives` 数组包含所有直播源：

```json
"lives": [
  {
    "name": "YanG•综合",
    "type": 0,
    "url": "https://tv.iill.top/m3u/Gather",
    "playerType": 2
  }
]
```

**提取规则**：
1. 遍历 `lives[]` 数组
2. 取每个元素的 `name`（显示名称）和 `url`（直播源地址）
3. `playerType` 字段：1=系统播放器，2=ExoPlayer（我们统一用 ExoPlayer）
4. 可选字段：`epg`（电子节目单）、`logo`（频道Logo）

#### 方法二：从 sites[] 中识别直播站点

部分仓库将直播源放在 `sites[]` 中，通过特征识别：

```json
{
  "key": "lf_js_lf_live",
  "name": "电视┃直播",
  "type": 3,
  "api": "./lib/lf_live_min.js",
  "ext": "./js/lf_live.txt"
}
```

**识别规则**：
1. name 包含"直播"关键词
2. api 包含 `live` 关键词
3. ext 字段指向 `.txt` 文件（通常是 IPTV 格式）

#### 方法三：URL 特征智能识别

对仓库中所有 URL 进行特征匹配：

| URL 特征 | 类型 | 示例 |
|----------|------|------|
| 含 `/api.php/provide/vod` | 影视站点 | `https://cj.lziapi.com/api.php/provide/vod/at/xml/` |
| 含 `.m3u8` 或 `.m3u` | 直播源 | `https://live.fanmingming.com/tv/m3u/ipv6.m3u` |
| 含 `.txt` | 可能是直播源（IPTV格式） | `http://home.jundie.top:81/Cat/tv/live.txt` |
| 含 `/m3u/` 路径 | 直播源 | `https://tv.iill.top/m3u/Gather` |
| 含 `live` 关键词 | 可能是直播源 | `https://tv.iill.top/m3u/Live` |

### 5.4 IPTV 格式说明

部分直播源使用 IPTV TXT 格式（非 M3U）：

```
#genre#
央视频道,CCTV1,http://xxx/CCTV1.m3u8
央视频道,CCTV2,http://xxx/CCTV2.m3u8
卫视频道,湖南卫视,http://xxx/hunan.m3u8
```

**解析规则**：
- 以 `#genre#` 为分隔符
- 每行格式：`分类,频道名,播放地址`
- 逗号分隔，播放地址可以是 m3u8/m3u/flv 等

### 5.5 直播源 M3U 格式说明

标准 M3U8 格式：

```
#EXTM3U x-tvg-url="https://live.fanmingming.cn/e.xml"
#EXTINF:-1 tvg-name="CCTV1" tvg-logo="https://..." group-title="央视频道",CCTV-1综合
http://xxx/CCTV1.m3u8
#EXTINF:-1 tvg-name="CCTV2" tvg-logo="https://..." group-title="央视频道",CCTV-2财经
http://xxx/CCTV2.m3u8
```

**解析规则**：
- `#EXTM3U`：文件头
- `#EXTINF`：节目信息（-1=无限时长）
- `tvg-name`：频道名称
- `tvg-logo`：频道Logo URL
- `group-title`：频道分组（用于分类Tab）
- 最后一行：播放地址

### 5.6 直播源提取完整流程

```
输入仓库地址
       ↓
请求仓库 JSON
       ↓
┌─────────────────────────────────┐
│ 提取 lives[] 中的直播源          │
│  ├─ name → 显示名称             │
│  ├─ url → 直播源地址             │
│  └─ logo/epg → 可选附加信息      │
└─────────────────────────────────┘
       ↓
┌─────────────────────────────────┐
│ 遍历 sites[] 识别直播站点        │
│  ├─ name 含"直播"               │
│  ├─ api 含 "live"               │
│  └─ ext 指向 .txt/.m3u 文件     │
└─────────────────────────────────┘
       ↓
┌─────────────────────────────────┐
│ 合并直播源列表                   │
│  ├─ 去重（按 URL）               │
│  └─ 保存到 SharedPreferences     │
└─────────────────────────────────┘
       ↓
用户选择直播源 → 请求 URL → 解析 M3U/IPTV → 显示频道列表
```

### 5.7 站点提取完整流程

```
输入仓库地址
       ↓
请求仓库 JSON
       ↓
┌─────────────────────────────────┐
│ 遍历 sites[] 提取影视站点        │
│  过滤条件：                       │
│  ├─ type == 0 (XML采集)          │
│  ├─ type == 1 (JSON采集)         │
│  └─ api 含 /api.php/provide/vod │
│  排除：                           │
│  ├─ type == 3 (JAR/JS插件)       │
│  ├─ api 以 csp_ 开头             │
│  └─ name 含 直播/弹幕/网盘等      │
└─────────────────────────────────┘
       ↓
┌─────────────────────────────────┐
│ 智能识别每个站点的 URL 特征      │
│  ├─ /api.php/provide/vod → MV   │
│  ├─ .m3u8/.m3u → LIVE           │
│  ├─ .txt → 可能是 LIVE          │
│  └─ 无明确特征 → 标记为"未知"    │
└─────────────────────────────────┘
       ↓
┌─────────────────────────────────┐
│ 合并站点列表                     │
│  ├─ 去重（按 URL）               │
│  ├─ 标记类型（MV/LIVE/未知）     │
│  └─ 保存到 SharedPreferences     │
└─────────────────────────────────┘
       ↓
用户选择站点 → 根据类型走不同流程：
  MV → 请求 ac=list → 获取分类 → 请求 ac=detail → 获取影片
  LIVE → 请求 M3U/TXT → 解析 → 获取频道列表
  未知 → 自动请求URL → 根据响应判断类型
```

### 5.8 常用公开仓库地址参考

| 仓库 | URL | 说明 |
|------|-----|------|
| 饭太硬 | `http://www.饭太硬.top:3456/api?code=default` | 含 lives + sites |
| 肥猫 | `http://我不是.肥猫.live:3721/api/v3/file/get/113403/%E8%82%A5%E7%8C%AB%E5%9C%B0%E5%9B%BE.json/bridge` | 含 lives + sites |
| 高天流云 | `https://raw.githubusercontent.com/gaotianliuyun/gao/master/0821.json` | 含 lives + sites |
| YanG | `https://tv.iill.top/json/yg.json` | 含 lives + sites |

| 直播源 | URL | 格式 |
|--------|-----|------|
| 范明明 | `https://live.fanmingming.com/tv/m3u/ipv6.m3u` | M3U |
| 肥猫 | `http://我不是.肥猫.live/TV/tvzb.txt` | IPTV TXT |
| YanG综合 | `https://tv.iill.top/m3u/Gather` | M3U |
| YanG直播 | `https://tv.iill.top/m3u/Live` | M3U |
| 俊于 | `http://home.jundie.top:81/Cat/tv/live.txt` | IPTV TXT |

---

## 六、完整端到端流程（开发实现指南）

### 6.1 影视播放全流程

```
Step 1: 用户输入仓库地址（如 https://raw.githubusercontent.com/gaotianliuyun/gao/master/0821.json）
   ↓
Step 2: OkHttp 请求仓库 JSON
   ↓
Step 3: 解析 JSON → 遍历 sites[]
   ├─ type == 0 → XML采集站点 → 加入影视站点列表
   ├─ type == 1 → JSON采集站点 → 加入影视站点列表
   ├─ type == 3 且 api 含 /api.php/provide/vod → 影视站点 → 加入列表
   ├─ type == 3 且 api 以 csp_ 开头 → JAR插件 → 跳过
   └─ type == 3 且 api 以 http 开头且 ext 指向 .js → drpy插件 → 跳过
   ↓
Step 4: 用户选择影视站点（如 非凡┃采集）
   ↓
Step 5: 请求 ac=list 获取分类
   URL: http://cj.ffzyapi.com/api.php/provide/vod/?ac=list
   返回: {code:1, class:[{type_id:1,type_name:"电影片"},{type_id:2,type_name:"连续剧"},...]}
   ↓
Step 6: 用户选择分类（如 电影片, type_id=1）
   ↓
Step 7: 请求 ac=detail&t={typeId}&pg={page} 获取影片列表
   URL: http://cj.ffzyapi.com/api.php/provide/vod/?ac=detail&t=1&pg=1
   返回: {code:1, list:[{vod_id:98488,vod_name:"三人行2026",vod_pic:"https://...",vod_year:"2024",vod_area:"大陆",vod_actor:"xxx",vod_director:"xxx",vod_content:"剧情简介...",vod_remarks:"HD",vod_play_from:"feifan$$$ffm3u8",vod_play_url:"第01集$https://...#第02集$https://..."}], class:[{type_id:1,type_name:"电影片"},...], total:1000, page:1, pagecount:50}
   ↓
Step 8: 用户点击影片卡片 → 进入详情页
   ↓
Step 9: 解析 vod_play_url
   格式: 线路1名称$集名1$播放链接#集名2$播放链接$$$线路2名称$集名1$播放链接#...
   分隔符: $$$ 分隔线路, # 分隔集数, $ 分隔集名和播放链接
   解析结果: [
     {line:"feifan", episodes:[{name:"第01集",url:"https://vip.ffzy-play8.com/share/xxx"},{name:"第02集",url:"https://vip.ffzy-play8.com/share/yyy"}]},
     {line:"ffm3u8", episodes:[{name:"第01集",url:"https://vip.ff-play8.com/m3u8/xxx"},...]}
   ]
   ↓
Step 10: 用户选择线路和集数 → 进入播放器
   ↓
Step 11: ExoPlayer 播放视频
   URL: https://vip.ffzy-play8.com/share/xxx (或直接 m3u8 地址)
   请求头: User-Agent: "Mozilla/5.0", Referer: 同源域名
   注意: 部分站点返回的播放链接需要拼接域名或添加 Cookie
```

**关键实现细节**：

1. **分类接口**：`ac=list` 返回 `class[]`，每个 class 有 `type_id`（数字ID）和 `type_name`（中文名称）
2. **影片列表接口**：`ac=detail&t={typeId}&pg={page}` 返回 `list[]`，分页加载
3. **搜索接口**：`ac=detail&wd={keyword}&pg={page}` 返回 `list[]`，格式同分类
4. **首页推荐**：`ac=detail`（无参数）返回首页推荐影片 + 分类列表
5. **vod_play_url 解析**：
   - `$$$` 分隔不同线路
   - `#` 分隔同一线路的不同集数
   - `$` 分隔集名和播放地址
   - 示例：`线路A$第1集$http://a.mp4#第2集$http://b.mp4$$$线路B$第1集$http://c.mp4`
6. **播放链接处理**：
   - 部分链接返回的是 HTML 页面（需二次解析）
   - 部分链接需要添加 Referer/User-Agent
   - 部分 m3u8 链接需要拼接域名

### 6.2 直播播放全流程

```
Step 1: 从仓库 JSON 中提取直播源
   ├─ 方法A: 遍历 lives[] 数组 → 取 name/url
   └─ 方法B: 遍历 sites[] → 识别 name 含"直播"或 ext 指向 .txt/.m3u 的站点
   ↓
Step 2: 用户选择直播源（如 "范明明•ipv6"）
   ↓
Step 3: 请求直播源 URL
   URL: https://live.fanmingming.com/tv/m3u/ipv6.m3u
   返回: M3U 格式文本
   ↓
Step 4: 解析 M3U 格式
   每两条记录为一组：
   #EXTINF:-1 tvg-name="CCTV1" tvg-logo="https://..." group-title="央视频道",CCTV-1综合
   http://xxx/CCTV1.m3u8
   
   解析结果: [
     {name:"CCTV-1综合", logo:"https://...", group:"央视频道", url:"http://xxx/CCTV1.m3u8"},
     {name:"CCTV-2财经", logo:"https://...", group:"央视频道", url:"http://xxx/CCTV2.m3u8"},
     ...
   ]
   ↓
Step 5: 按 group-title 分组显示分类Tab（央视频道/卫视频道/地方频道等）
   ↓
Step 6: 用户选择频道 → 进入播放器
   ↓
Step 7: ExoPlayer 播放 m3u8 直播流
   URL: http://xxx/CCTV1.m3u8
   请求头: User-Agent: "Mozilla/5.0"
   ↓
Step 8: （可选）使用 EPG 数据展示节目单
   URL: http://epg.112114.xyz/?ch={频道名}&date={日期}
```

**IPTV TXT 格式解析**：

```
综合,#genre#
CCTV1,http://xxx/CCTV1.m3u8
CCTV2,http://xxx/CCTV2.m3u8
卫视频道,#genre#
湖南卫视,http://xxx/hunan.m3u8
```

解析规则：
- `#genre#` 为分类分隔符
- 每行格式：`频道名,播放地址`
- 分类名即为 `#genre#` 前面的文字

### 6.3 数据模型定义

```java
// 站点信息
class SiteInfo {
    String name;          // 站点名称，如 "非凡┃采集"
    String api;           // API 地址，如 "http://cj.ffzyapi.com/api.php/provide/vod"
    int type;             // 0=XML, 1=JSON, 2=EXML, 3=JAR/JS
    String typeLabel;     // "MV" / "LIVE" / "UNKNOWN"
    boolean isLive;       // 是否为直播源
}

// 分类信息
class CategoryInfo {
    String typeId;        // 类型ID，如 "1"
    String typeName;      // 类型名称，如 "电影片"
    int typePid;          // 父类ID，0=一级分类
}

// 影片信息
class MovieItem {
    String vodId;         // 影片ID
    String title;         // 标题
    String pic;           // 海报URL
    String tag;           // 标签，如 "HD" / "更新至第02集"
    String type;          // 类型名称
    String year;          // 年份
    String area;          // 地区
    String actor;         // 主演
    String director;      // 导演
    String content;       // 剧情简介
    String score;         // 评分
    String playFrom;      // 播放来源，如 "feifan$$$ffm3u8"
    String playUrl;       // 播放URL字符串
}

// 线路信息
class PlayLine {
    String lineName;      // 线路名称，如 "非凡专线"
    List<Episode> episodes;  // 剧集列表
}

// 剧集信息
class Episode {
    String name;          // 集名，如 "第01集"
    String url;           // 播放地址
}

// 直播频道
class LiveChannel {
    String name;          // 频道名称，如 "CCTV-1综合"
    String logo;          // 频道Logo URL
    String group;         // 分组，如 "央视频道"
    String url;           // 播放地址
}

// 直播源
class LiveSource {
    String name;          // 直播源名称，如 "范明明•ipv6"
    String url;           // 直播源地址
    String epg;           // EPG 地址（可选）
    String logoTemplate;  // Logo 模板（可选，{name} 占位符）
}
```

### 6.4 OkHttp 请求封装

```java
// 统一请求头
private static final OkHttpClient client = new OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .writeTimeout(15, TimeUnit.SECONDS)
    .followRedirects(false)
    .followSslRedirects(false)
    .build();

private static final Headers HEADERS = new Headers.Builder()
    .add("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
    .build();

// 请求分类列表
public Observable<List<CategoryInfo>> loadCategories(String baseUrl) {
    String url = baseUrl + "?ac=list";
    return OkHttpUtil.getObservable(url, HEADERS)
        .map(response -> parseCategories(response));
}

// 请求影片列表
public Observable<List<MovieItem>> loadMovies(String baseUrl, String typeId, int page) {
    String url = baseUrl + "?ac=detail&t=" + typeId + "&pg=" + page;
    return OkHttpUtil.getObservable(url, HEADERS)
        .map(response -> parseMovies(response));
}

// 请求搜索
public Observable<List<MovieItem>> search(String baseUrl, String keyword, int page) {
    String url = baseUrl + "?ac=detail&wd=" + Uri.encode(keyword) + "&pg=" + page;
    return OkHttpUtil.getObservable(url, HEADERS)
        .map(response -> parseMovies(response));
}

// 请求首页推荐
public Observable<HomePageData> loadHomePage(String baseUrl) {
    String url = baseUrl + "?ac=detail";
    return OkHttpUtil.getObservable(url, HEADERS)
        .map(response -> parseHomePage(response));
}

// 请求直播源
public Observable<String> loadLiveSource(String url) {
    return OkHttpUtil.getObservable(url, HEADERS);
}

// 请求仓库JSON
public Observable<RepoConfig> loadRepoConfig(String url) {
    return OkHttpUtil.getObservable(url, HEADERS)
        .map(response -> parseRepoConfig(response));
}
```

### 6.5 JSON 解析实现

```java
// 解析仓库配置
class RepoConfig {
    List<LiveSource> lives;     // 直播源列表
    List<SiteInfo> sites;       // 站点列表
    String spider;              // 蜘蛛jar地址（暂不处理）
    String wallpaper;           // 壁纸地址（暂不处理）
}

// 解析分类列表
private List<CategoryInfo> parseCategories(String json) {
    Gson gson = new Gson();
    JsonObject root = gson.fromJson(json, JsonObject.class);
    JsonArray classes = root.getAsJsonArray("class");
    List<CategoryInfo> result = new ArrayList<>();
    for (JsonElement el : classes) {
        CategoryInfo c = new CategoryInfo();
        c.typeId = el.getAsJsonObject().get("type_id").getAsString();
        c.typeName = el.getAsJsonObject().get("type_name").getAsString();
        c.typePid = el.getAsJsonObject().get("type_pid").getAsInt();
        result.add(c);
    }
    return result;
}

// 解析影片列表
private List<MovieItem> parseMovies(String json) {
    Gson gson = new Gson();
    JsonObject root = gson.fromJson(json, JsonObject.class);
    JsonArray list = root.getAsJsonArray("list");
    List<MovieItem> result = new ArrayList<>();
    for (JsonElement el : list) {
        MovieItem m = new MovieItem();
        m.vodId = el.getAsJsonObject().get("vod_id").getAsString();
        m.title = el.getAsJsonObject().get("vod_name").getAsString();
        m.pic = el.getAsJsonObject().get("vod_pic").getAsString();
        m.tag = el.getAsJsonObject().get("vod_remarks").getAsString();
        m.year = el.getAsJsonObject().get("vod_year").getAsString();
        m.area = el.getAsJsonObject().get("vod_area").getAsString();
        m.actor = el.getAsJsonObject().get("vod_actor").getAsString();
        m.director = el.getAsJsonObject().get("vod_director").getAsString();
        m.content = el.getAsJsonObject().get("vod_content").getAsString();
        m.playFrom = el.getAsJsonObject().get("vod_play_from").getAsString();
        m.playUrl = el.getAsJsonObject().get("vod_play_url").getAsString();
        result.add(m);
    }
    return result;
}

// 解析播放链接
private List<PlayLine> parsePlayLines(String playFrom, String playUrl) {
    List<PlayLine> lines = new ArrayList<>();
    String[] froms = playFrom.split("\\$\\$\\$");
    String[] urls = playUrl.split("\\$\\$\\$");
    for (int i = 0; i < froms.length && i < urls.length; i++) {
        PlayLine line = new PlayLine();
        line.lineName = froms[i].trim();
        line.episodes = new ArrayList<>();
        String[] episodes = urls[i].split("#");
        for (String ep : episodes) {
            String[] parts = ep.split("\\$", 2);
            if (parts.length == 2) {
                Episode e = new Episode();
                e.name = parts[0].trim();
                e.url = parts[1].trim();
                line.episodes.add(e);
            }
        }
        lines.add(line);
    }
    return lines;
}

// 解析 M3U 直播源
private List<LiveChannel> parseM3U(String m3uText) {
    List<LiveChannel> channels = new ArrayList<>();
    Scanner scanner = new Scanner(m3uText);
    LiveChannel current = null;
    while (scanner.hasNextLine()) {
        String line = scanner.nextLine().trim();
        if (line.startsWith("#EXTINF")) {
            current = new LiveChannel();
            // 提取 tvg-name
            Matcher nameMatcher = Pattern.compile("tvg-name=\"([^\"]+)\"").matcher(line);
            if (nameMatcher.find()) current.name = nameMatcher.group(1);
            // 提取 tvg-logo
            Matcher logoMatcher = Pattern.compile("tvg-logo=\"([^\"]+)\"").matcher(line);
            if (logoMatcher.find()) current.logo = logoMatcher.group(1);
            // 提取 group-title
            Matcher groupMatcher = Pattern.compile("group-title=\"([^\"]+)\"").matcher(line);
            if (groupMatcher.find()) current.group = groupMatcher.group(1);
            // 提取频道显示名（逗号后面的部分）
            int commaIdx = line.lastIndexOf(",");
            if (commaIdx >= 0) current.name = line.substring(commaIdx + 1).trim();
        } else if (line.startsWith("http")) {
            if (current != null) {
                current.url = line;
                if (current.name != null && !current.name.isEmpty()) {
                    channels.add(current);
                }
                current = null;
            }
        }
    }
    scanner.close();
    return channels;
}

// 解析 IPTV TXT 直播源
private List<LiveChannel> parseIptvTxt(String txt) {
    List<LiveChannel> channels = new ArrayList<>();
    String currentGroup = "默认";
    for (String line : txt.split("\n")) {
        line = line.trim();
        if (line.isEmpty() || line.equals("#EXTM3U")) continue;
        if (line.contains("#genre#")) {
            currentGroup = line.replace("#genre#", "").trim();
            continue;
        }
        String[] parts = line.split(",");
        if (parts.length >= 2) {
            LiveChannel ch = new LiveChannel();
            ch.name = parts[0].trim();
            ch.url = parts[1].trim();
            ch.group = currentGroup;
            channels.add(ch);
        }
    }
    return channels;
}
```

### 6.6 智能链接识别实现

```java
class LinkTypeDetector {
    
    /**
     * 判断 URL 类型
     * @return 0=MV影视, 1=LIVE直播, -1=未知
     */
    public static int detectType(String url) {
        if (url == null || url.isEmpty()) return -1;
        
        // 影视站点特征
        if (url.contains("/api.php/provide/vod") ||
            url.contains("/api.php/provide/") ||
            url.contains("cj.") && url.contains("api")) {
            return 0;
        }
        
        // 直播源特征
        if (url.contains(".m3u8") ||
            url.contains(".m3u") ||
            url.endsWith(".txt") ||
            url.contains("/m3u/") ||
            url.contains("/live/") ||
            url.contains("/tv/")) {
            return 1;
        }
        
        // 根据 URL 响应内容判断
        try {
            Response response = OkHttpUtil.head(url, HEADERS, 5000);
            String contentType = response.header("Content-Type", "");
            if (contentType.contains("application/vnd.apple.mpegurl") ||
                contentType.contains("audio/x-mpegurl") ||
                contentType.contains("text/plain")) {
                // 下载内容检查
                String body = OkHttpUtil.get(url, HEADERS, 5000).body().string();
                if (body.startsWith("#EXTM3U") || body.contains("#EXTINF") ||
                    body.contains("#genre#")) {
                    return 1; // M3U 或 IPTV TXT
                } else if (body.contains("\"class\"") || body.contains("\"list\"")) {
                    return 0; // TVBox JSON API
                }
            }
        } catch (Exception e) {
            // 忽略
        }
        
        return -1; // 未知
    }
    
    /**
     * 从仓库 JSON 中提取站点
     */
    public static List<SiteInfo> extractSites(JsonObject repoJson) {
        List<SiteInfo> sites = new ArrayList<>();
        JsonArray jsonSites = repoJson.getAsJsonArray("sites");
        
        for (JsonElement el : jsonSites) {
            JsonObject site = el.getAsJsonObject();
            String name = site.get("name").getAsString();
            String api = site.get("api").getAsString();
            int type = site.has("type") ? site.get("type").getAsInt() : -1;
            
            // 只提取 type=0 和 type=1 的采集站点
            if (type == 0 || type == 1) {
                SiteInfo info = new SiteInfo();
                info.name = name;
                info.api = api;
                info.type = type;
                info.isLive = false;
                info.typeLabel = "MV";
                sites.add(info);
            }
            // type=3 的站点，检查 api 是否包含 /api.php/provide/vod
            else if (type == 3 && api.contains("/api.php/provide/vod")) {
                SiteInfo info = new SiteInfo();
                info.name = name;
                info.api = api;
                info.type = 1;
                info.isLive = false;
                info.typeLabel = "MV";
                sites.add(info);
            }
        }
        return sites;
    }
    
    /**
     * 从仓库 JSON 中提取直播源
     */
    public static List<LiveSource> extractLives(JsonObject repoJson) {
        List<LiveSource> lives = new ArrayList<>();
        JsonArray jsonLives = repoJson.getAsJsonArray("lives");
        
        if (jsonLives == null) return lives;
        
        for (JsonElement el : jsonLives) {
            JsonObject live = el.getAsJsonObject();
            LiveSource info = new LiveSource();
            info.name = live.get("name").getAsString();
            info.url = live.get("url").getAsString();
            if (live.has("epg")) info.epg = live.get("epg").getAsString();
            if (live.has("logo")) info.logoTemplate = live.get("logo").getAsString();
            lives.add(info);
        }
        return lives;
    }
}
```

---

## 十七、数据流

### 5.1 智能链接识别机制

仓库JSON中的每个站点包含 `type` 字段（或根据URL特征自动识别）：

| 类型 | 识别方式 | 处理方式 |
|------|----------|----------|
| 影视站点 (MV) | `type=0` 或 URL含 `/api.php/provide/vod` 或 `/api.php/provide` | 走TVBox API流程 |
| 直播源 (LIVE) | `type=1` 或 URL含 `.m3u8`/`.m3u`/`.txt` | 走M3U直播源流程 |
| 未知类型 | 无明确标识 | 自动请求URL，根据响应内容智能判断 |

```
仓库地址输入
       ↓
  请求 JSON 配置
       ↓
解析配置 → 遍历 sites[]，智能识别每个站点类型
       ↓
  type=MV → 影视站点 → 走TVBox API
  type=LIVE → 直播源 → 走M3U解析
  type=0 → 影视站点 → 走TVBox API
  type=1 → 直播源 → 走M3U解析
  无type → 自动请求URL → 根据响应判断
       ↓
影视站点 → 请求 ac=list → 获取 class[] → 选择分类 → 请求 ac=detail → 获取影片列表
直播源 → 请求 M3U 文本 → 解析 channels[] → 选择频道 → ExoPlayer 播放
```

### 5.2 影视数据流

```
用户选择影视站点
       ↓
  请求 ac=list → 获取 class[]（分类列表）
       ↓
选择分类 → 请求 ac=detail&t={typeId}&pg={page} → 获取 list[]（影片列表）
       ↓
选择影片 → 请求 ac=videolist&ids={vodId} → 获取 vod_play_url（剧集）
       ↓
选择剧集 → ExoPlayer 播放 m3u8/mp4 视频流
```

### 5.3 直播数据流

```
仓库JSON解析 / 手动添加
       ↓
识别 type=1 / LIVE / .m3u8/.m3u/.txt  → 自动导入直播源
手动输入 M3U URL                        → 添加自定义直播源
       ↓
合并到直播源列表 (SharedPreferences)
       ↓
选择直播源 → 请求 M3U 文本
       ↓
解析 M3U → 获取 groups[]（分类组）+ channels[]（频道列表）
       ↓
选择分类 → 获取频道列表
       ↓
选择频道 → ExoPlayer 播放 m3u8 直播流
```

---

## 十八、TVBox API 格式与直播源格式

### 6.1 TVBox API 协议（影视站点）

所有影视源遵循统一的 TVBox API 协议：

| 接口 | 参数 | 返回 |
|------|------|------|
| 首页推荐 | `ac=detail` | `{code:1, list:[{vod_id,vod_name,vod_pic,...}], class:[{type_id,type_name}], total:N}` |
| 分类列表 | `ac=list` | `{class:[{type_id,type_name,type_pid}]}` |
| 分类影片 | `ac=detail&t={typeId}&pg={page}` | 同首页推荐 |
| 搜索 | `ac=detail&wd={keyword}&pg={page}` | 同首页推荐 |
| 影片详情 | `ac=videolist&ids={vod_id}` | `{list:[{vod_play_from,vod_play_url}]}` |

**API 示例**：
```
http://www.饭太硬.net/tv         ← 仓库地址
http://www.饭太硬.com/tv         ← 备用仓库
http://100km.top/0               ← 备用仓库

http://cj.ffzyapi.com/api.php/provide/vod   ← 非凡采集（直接API）
https://bfzyapi.com/api.php/provide/vod/    ← 暴风采集
```

### 6.2 M3U 直播源格式（直播站点）

直播源采用标准 M3U/M3U8 格式：

```
#EXTM3U
#EXTINF:-1 tvg-id="CCTV1" tvg-name="CCTV1" tvg-logo="https://..." group-title="央视",CCTV1 综合
http://live.example.com/cctv1.m3u8
#EXTINF:-1 tvg-id="CCTV2" tvg-name="CCTV2" tvg-logo="https://..." group-title="央视",CCTV2 财经
http://live.example.com/cctv2.m3u8
#EXTINF:-1 tvg-id="Hunan" tvg-name="湖南卫视" tvg-logo="https://..." group-title="卫视",湖南卫视
http://live.example.com/hunan.m3u8
```

**字段说明**：
- `#EXTM3U`：文件头
- `#EXTINF`：节目信息（-1=无限时长）
- `tvg-id`：频道唯一标识
- `tvg-name`：频道名称
- `tvg-logo`：频道Logo URL
- `group-title`：频道分组（央视/卫视/地方）
- 最后一行：直播流 URL（m3u8/m3u/flv 等）

---

## 十九、技术选型

### 7.1 核心依赖

| 组件 | 技术 | 用途 |
|------|------|------|
| UI框架 | Material Design 1.10.0 | 按钮、卡片、导航 |
| 播放器 | Media3 ExoPlayer 1.3.1 | 视频播放 |
| 播放器UI | Media3 UI 1.3.1 | 控制栏、进度条 |
| 网络请求 | OkHttp 4.12.0 | HTTP API调用 |
| JSON解析 | Gson 2.10.1 | API响应解析 |
| 图片加载 | Glide 4.16.0 | 海报/频道Logo加载 |
| 列表控件 | RecyclerView + GridLayoutManager | 影片/频道网格 |
| 横向滚动 | ViewPager2 / HorizontalScrollView | 分类Tab |
| 本地存储 | SharedPreferences | 用户偏好、站点配置 |
| 本地数据库 | Room 2.6.1 | 播放历史/收藏 |

### 7.2 配色方案

| 用途 | 颜色 | 十六进制 |
|------|------|----------|
| 页面背景 | 深蓝黑 | `#05070D` |
| 卡片背景 | 深蓝灰 | `#141E2E` |
| 次要背景 | 更深的蓝 | `#101D2B` |
| 主文字 | 白色 | `#FFFFFF` |
| 次要文字 | 浅灰 | `#8899AA` |
| 弱化文字 | 灰蓝 | `#69748D` |
| 强调色 | 紫色 | `#7C3AED` |
| 评分/亮点 | 橙色 | `#FFA500` |

### 7.3 圆角规范

| 元素 | 圆角 |
|------|------|
| 卡片 | 12dp |
| 按钮 | 22dp |
| 输入框 | 12dp |
| 海报 | 14dp |
| 弹窗 | 22dp |

---

## 二十、实现优先级

### Phase 1: 基础架构 (最高优先级)
1. MainActivity + 底部导航 (5个Tab)
2. MovieHomeFragment + MovieGridAdapter
3. MovieApi 基础方法（首页/分类）
4. MovieDetailActivity
5. MoviePlayerActivity (ExoPlayer)
6. 构建验证

### Phase 2: 直播模块 (高优先级)
7. LiveHomeFragment + LiveGridAdapter
8. LiveApi + LiveParser (M3U解析)
9. LivePlayerActivity (ExoPlayer)
10. 频道切换功能

### Phase 3: 仓库管理与智能识别 (高优先级)
11. RepoManageActivity (仓库管理)
12. SiteSelectorDialog (站点选择，支持MV/LIVE标识)
13. LinkTypeDetector (智能链接识别器)
14. 长按影视Tab 600ms触发仓库管理
15. 站点切换功能 + 上次站点恢复

### Phase 4: 历史与收藏 (中优先级)
15. HistoryFragment + FavoriteFragment
16. Room 数据库 (历史/收藏实体)
17. 播放记录保存
18. 收藏/取消收藏功能

### Phase 5: 搜索与设置 (中优先级)
19. MovieSearchActivity
20. SettingsFragment (播放器/仓库/直播源/搜索/个性化)
21. 缓存机制 (SharedPreferences)
22. 下拉加载更多
23. 骨架屏/加载状态

### Phase 6: 彩票模块 (保留)
24. LotteryFragment + WebView
25. AndroidJSBridge 桥接
26. 构建验证

---

## 二十一、与现有代码的对比

### 现有 (WebView) vs 目标 (纯原生)

| 功能 | 现有实现 | 目标实现 |
|------|----------|----------|
| 首页 | main.html + nc-movie-engine.js | MovieHomeFragment (RecyclerView) |
| 分类Tab | JS动态生成 | 原生 HorizontalScrollView + Button |
| 影片网格 | CSS Grid | RecyclerView + GridLayoutManager(3) |
| 搜索 | JS搜索框 | MovieSearchActivity |
| 详情 | JS弹窗 | MovieDetailActivity |
| 播放器 | ArtPlayer (JS) | ExoPlayer (原生) |
| 仓库管理 | nc-repo.js + localStorage | RepoManageActivity + SharedPreferences |
| 站点切换 | JS选择器 | SiteSelectorDialog |
| 海报加载 | CSS background-image | Glide |
| 彩票 | main.html + nc-lottery.js | LotteryFragment (WebView) |
| 直播 | 无 | LiveHomeFragment + LivePlayerActivity |
| 历史/收藏 | 无 | HistoryFragment + FavoriteFragment + Room |
| 设置 | 无 | SettingsFragment |
| 链接识别 | 手动选择 | 智能识别 (MV/LIVE/自动) |
| 直播源管理 | 无 | 仓库自动导入 + 手动添加/删除 |

---

## 二十二、风险与注意事项

1. **API兼容性**：不同影视源的API返回格式可能略有差异，需要做好容错处理
2. **图片URL处理**：部分API返回相对路径URL，需要拼接域名
3. **播放链接解析**：vod_play_url格式可能包含多条线路，需要正确解析
4. **网络权限**：Android 9+ 需要 cleartextTraffic 配置（已在 Manifest 中设置）
5. **ExoPlayer版本**：使用 Media3 ExoPlayer 1.3.1，注意 API 变更
6. **内存管理**：Glide 图片加载需注意生命周期管理
7. **线程安全**：网络请求必须在子线程，UI更新必须在主线程
8. **M3U解析**：直播源M3U格式可能有多种变体，需要兼容处理
9. **智能识别准确性**：URL特征识别可能有误判，需要提供手动切换机制
10. **仓库JSON格式多样性**：不同仓库的JSON结构可能不同，需要兼容多种格式

---

## 二十三、构建配置

### build.gradle 新增依赖

```groovy
// Media3 ExoPlayer
implementation "androidx.media3:media3-exoplayer:1.3.1"
implementation "androidx.media3:media3-ui:1.3.1"
implementation "androidx.media3:media3-exoplayer-hls:1.3.1"

// Room 数据库
implementation "androidx.room:room-runtime:2.6.1"
annotationProcessor "androidx.room:room-compiler:2.6.1"
```

### AndroidManifest.xml 新增 Activity

```xml
<activity android:name=".ui.MovieSearchActivity" />
<activity android:name=".ui.MovieDetailActivity" />
<activity android:name=".ui.MoviePlayerActivity" />
<activity android:name=".ui.LivePlayerActivity" />
<activity android:name=".ui.RepoManageActivity" />
<activity android:name=".ui.SettingsActivity" />
```

### versionCode 更新
- 从 v12.0 (91) → v13.0 (93)

---

## 十八、公开仓库功能对比与额外功能分析

### 18.1 仓库JSON完整字段分析

通过对 0821.json 的完整分析，仓库JSON包含以下顶级字段：

| 字段 | 类型 | 说明 | 是否必需 |
|------|------|------|---------|
| spider | string | 蜘蛛jar地址，用于增强采集站功能（含MD5校验） | 否 |
| logo | string | 应用图标URL | 否 |
| lives | array | 直播源列表 | 否 |
| wallpaper | string | 壁纸API地址 | 否 |
| sites | array | 站点列表（核心） | 是 |
| parses | array | 备用解析列表 | 否 |
| flags | array | 播放标志列表（61个），用于关联解析 | 否 |
| doh | array | 安全DNS列表 | 否 |
| rules | array | 规则列表（m3u8去广告、直链替换等） | 否 |

### 18.2 站点类型详解

#### 类型0/1：传统采集站（MV影视）
- `type=0`：XML采集（如 量子、非凡）
- `type=1`：JSON采集（如 百度、海外看、暴風、索尼、快帆）
- API特征：`/api.php/provide/vod/`
- 支持功能：分类列表、影片列表、搜索、详情播放
- **本项目需要实现**

#### 类型3：JAR/JS插件（csp_开头）
- **csp_WoGG**（玩偶哥哥）：4K网盘搜索，需Cloud-drive配置
- **csp_MIPanSo/csp_PanSso/csp_PanSearch**（米盘搜/盘他/夸搜）：网盘搜索
- **csp_Libvio**（立播）：秒播网盘资源
- **csp_AList**：AList网盘聚合
- **csp_Bili**（哔哩）：B站内容，需ext JSON配置
- **csp_Push**（手机推送）：推送网盘链接到APP
- **csp_NewCz**（厂长）：直连采集站
- **csp_Zxzj**（在线）：在线之家直连
- **csp_SixV**（新6V）：磁力下载
- **csp_Dm84**（巴士动漫）：动漫
- **csp_Ysj**（异界动漫）：动漫
- **csp_Anime1**（日本动漫）：动漫
- **csp_Kekys**（可可）：多线采集
- **csp_Auete**（奥特）：无广告采集
- **csp_NanGua**（南瓜）：多线采集
- **csp_AppTT**（热播/萌米/欢视）：APP直连
- **csp_AppSx**（木星/速播）：APP直连
- **csp_Bbb**（天天）：APP直连
- **csp_Djtt**（短剧）：短剧
- **csp_FirstAid**（急救）：医学教学
- **csp_Alllive**（一直播）：直播
- **csp_WoGG**（玩偶哥哥）：4K弹幕
- **csp_YGP**（预告片）：新片预告
- **csp_DouDou**（豆瓣）：豆瓣信息
- **csp_kanqiu926**（926看球）：体育直播
- **csp_Bili**（明星MV）：B站MV

**结论**：
- 类型3的JAR插件需要WebView加载JAR或集成drpy引擎，**v13.0暂不支持**
- 但需要在站点列表中**显示并标注**这些站点的类型
- `csp_Bili`（哔哩）需要特殊的ext JSON配置，支持B站视频分类播放
- `csp_Push`（手机推送）需要支持推送链接功能
- `csp_SixV`（新6V）是磁力下载站点，需要种子播放支持

#### 特殊ext字段站点

| 站点 | ext内容 | 说明 |
|------|---------|------|
| 玩偶哥哥 | `Cloud-drive` + `siteUrl` + `danMu` | 网盘搜索+弹幕 |
| 米盘搜搜 | `Cloud-drive` | 百度网盘搜索 |
| 夸搜狸夸 | `pan:quark` + `Cloud-drive` | 夸克网盘搜索 |
| 立播秒播 | `Cloud-drive` | Libvio网盘 |
| 盘他三盘 | `Cloud-drive` | 多网盘聚合 |
| 哔哩合集 | `./json/chuqiuyu.json` | B站分类配置 |
| 哔哩课堂 | `./json/xuexi.json` | B站教育配置 |
| 明星MV | `json: MTV.json URL` | B站MV配置 |
| AList网盘 | `./json/alist.json` | AList服务器配置 |
| 手机推送 | `Cloud-drive` | 推送链接 |
| 在线秒播 | `https://www.zxzjhd.com/` | 指定网站域名 |
| 奥特无广 | `https://auete.com/` | 指定网站域名 |
| 新6V磁力 | `http://www.xb6v.com/` | 指定网站域名 |

**Cloud-drive.txt 作用**：存储网盘登录凭证（Cookie/Token），用于访问需要登录的网盘资源。

### 18.3 备用解析（parses）功能分析

仓库JSON中的 `parses[]` 提供视频解析服务：

| 名称 | URL | type | 说明 |
|------|-----|------|------|
| Json聚合 | Demo | 3 | 聚合多个解析源 |
| 虾米 | xmflv.com | 0 | 支持flag匹配 |
| PM | playm3u8.cn | 0 | 支持flag+header |
| m3u8 | m3u8.tv | 0 | 基础m3u8解析 |
| 8090 | 8090.la | 0 | 支持flag匹配 |
| 看看 | m3u8.pw | 0 | 基础解析 |
| 咸鱼 | xyflv.cc | 0 | 带header+flag |
| 云解析 | yparse.com | 0 | 带header |
| 爱豆 | aidouer.net | 0 | 带header+referer |
| 巧技 | qiaoji8.com/neibu | 1 | **服务端解析** |
| 巧技二 | qiaoji8.com/gouzi | 1 | **服务端解析** |

**type字段含义**：
- `type=0`：客户端解析（浏览器跳转jx页面）
- `type=1`：服务端解析（返回JSON `url`字段包含真实播放地址）
- `type=2`：PHP解析
- `type=3`：JSON解析（聚合）

**flag字段作用**：
- 用于匹配 `flags[]` 数组中的播放标志
- 不同解析器支持不同的flag（优酷/腾讯/爱奇艺等）
- 播放时根据 `vod_play_from` 匹配对应的解析器

**本项目策略**：
- v13.0 **实现type=0客户端解析**：点击解析按钮 → WebView打开jx页面 → 用户观看
- v13.0 **不实现type=1服务端解析**（巧技等需要特定服务器）
- 在播放页面提供"解析"按钮，列出所有可用的type=0解析器
- 用户可以**自定义添加/删除解析器**

### 18.4 flags（播放标志）分析

仓库JSON中的 `flags[]` 包含61个播放标志：
- 优酷系：youku、优酷、优 酷、优酷视频
- 腾讯系：qq、腾讯、腾 讯、腾讯视频
- 爱奇艺系：iqiyi、qiyi、奇艺、爱 奇 艺
- 其他：m1905、xigua、letv、leshi、乐视、乐 视、sohu、土豆、pptv、PPTV、bilibili、哔哩哔哩、哔哩、imgo、芒果、rx、ltnb、1905、tnmb、seven、NetFilx、fun、风行

**作用**：
- 匹配影片的 `vod_play_from` 字段
- 根据匹配的flag选择对应的解析器
- 例如：`vod_play_from="优酷$$$腾讯"` → 匹配flag"优酷"→ 使用虾米解析（支持youku flag）

**本项目实现**：
- 从仓库JSON加载flags
- 解析每个站点的 `vod_play_from`，与flags匹配
- 为每个线路自动推荐支持的解析器
- 也支持用户手动选择解析器

### 18.5 rules（规则）分析

仓库JSON中的 `rules[]` 包含视频播放规则：

| 名称 | hosts | regex | 说明 |
|------|-------|-------|------|
| kk | kuaikan | 5, 20.123... | 快看m3u8切割规则 |
| yqk | yqk | 18.4, 15.1666... | 云播放m3u8切割 |
| sn | suonizy | #EXTINF...original.ts | 索尼直链替换 |
| bf | bfzy | #EXT-X-DISCONTINUITY | 暴风m3u8去广告 |
| xx | aws.ulivetv.net | #EXT-X-DISCONTINUITY | 其他m3u8切割 |
| lz | vip.lz/hd.lz/v.cdnlz | 18.5333, 19.52... | 量子/非凡长度识别 |
| ff | vip.ffzy/hd.ffzy/ffzy | 25.0666, 25.08... | 非凡长度识别 |
| hs | huoshan.com | item_id= | 火山短链替换 |
| dy | douyin.com | is_play_url= | 抖音直链替换 |
| nm | toutiaovod.com | video/tos/cn | 头条直链替换 |
| cl | magnet | 最新, 直播, 更新 | 磁力搜索关键词 |

**作用**：
- `hosts`：匹配域名，触发对应规则
- `regex`：正则表达式，用于替换或识别视频直链
- 主要解决：m3u8分段识别、去广告、直链替换、短链解析

**本项目策略**：
- v13.0 **实现基本规则**：lz/ff（量子/非凡m3u8识别）、hf（火山短链）、dy（抖音）、nm（头条）
- 这些规则用于自动识别和替换需要VIP的m3u8链接为直链
- 其他高级规则延后实现

### 18.6 wallpaper（壁纸）分析

仓库JSON中的 `wallpaper` 字段：
- 值：`https://深色壁纸.xxooo.cf/` 或类似API地址
- 作用：设置App首页背景壁纸
- 常见壁纸API：
  - `https://api.vvhan.com/api/wallpaper/acgjj` → 返回 `{img: url}` 动漫壁纸
  - `https://api.btstu.cn/sjbz/api.php` → 返回随机壁纸
  - `https://picsum.photos/1080/1920` → 返回随机图片

**本项目实现**：
- 读取仓库JSON中的wallpaper字段
- 在首页（影视分类页）作为背景显示
- 提供"换壁纸"按钮，调用壁纸API获取新壁纸
- 使用Glide加载壁纸图片

### 18.7 doh（安全DNS）分析

仓库JSON中的 `doh[]` 字段：
- 包含DNS-over-HTTPS配置
- 作用：保护DNS查询隐私，防止DNS劫持
- 常见DNS：Google、Cloudflare、AdGuard、Quad9

**本项目策略**：
- v13.0 **暂不实现**DOH
- 使用系统默认DNS即可

### 18.8 spider（蜘蛛jar）分析

仓库JSON中的 `spider` 字段：
- 值：`"./jar/fan.txt;md5;6c4ab3a9d232164c75534f9060506ee5"`
- 格式：`jar地址;md5;MD5校验值`
- 作用：下载并加载增强jar，用于：
  - 破解某些站的反爬
  - 支持更多采集源
  - 直链替换（如量子/非凡的m3u8转直链）

**本项目策略**：
- v13.0 **暂不实现**spider jar加载
- 但需要记录spider地址，提示用户jar增强功能

### 18.9 Cloud-drive（网盘登录）分析

部分站点（玩偶哥哥、米盘搜搜、立播等）的ext中包含：
```json
{'Cloud-drive': 'tvfan/Cloud-drive.txt', 'from': '4k|auto'}
```

**Cloud-drive.txt 内容**：存储网盘Cookie/Token
```
{"quark": "cookie=...", "aliyun": "token=...", "baidu": "cookie=..."}
```

**作用**：
- 访问需要登录的网盘资源（夸克/阿里/百度）
- 提供更高画质和更多资源
- 4K资源通常需要网盘登录

**本项目策略**：
- v13.0 **不实现网盘登录**（需要WebView交互扫码）
- 但在设置中预留"网盘配置"入口
- 支持手动输入Cloud-drive.txt内容
- 标注哪些站点需要网盘登录

### 18.10 AList（网盘聚合）分析

`csp_AList` 站点使用 `./json/alist.json` 配置：
```json
{
  "vodPic": "https://x.imgs.ovh/x/2023/09/05/64f680bb030b4.png",
  "drives": [
    {"name": "弱水", "server": "http://shicheng.wang:555/"},
    {"name": "NICS", "server": "https://nics.eu.org"},
    {"name": "ECVE", "server": "https://pan.ecve.cn"},
    {"name": "小雅", "server": "http://alist.xiaozya.com/"}
  ]
}
```

**作用**：
- 聚合多个AList网盘服务器
- 浏览网盘中的视频文件
- 直接播放网盘中的mp4/m3u8文件

**本项目策略**：
- v13.0 **不实现**AList功能
- 但在设置中预留"添加AList服务器"入口

### 18.11 B站（Bili）内容分析

`csp_Bili` 站点使用ext JSON配置：
```json
{
  "cookie": "http://127.0.0.1:9978/file/TV/cookie.txt",
  "classes": [
    {"type_name": "帕梅拉", "type_id": "帕梅拉"},
    {"type_name": "太极拳", "type_id": "太极拳"},
    {"type_name": "短剧", "type_id": "短剧"}
  ],
  "filter": {
    "短剧": [
      {"key": "order", "name": "排序", "value": [
        {"n": "综合排序", "v": "0"},
        {"n": "最新发布", "v": "pubdate"}
      ]}
    ]
  }
}
```

**作用**：
- 播放B站视频内容
- 需要bili_cookie（通过JS盒子API获取）
- 支持分类、筛选、排序

**本项目策略**：
- v13.0 **不实现**B站内容（需要JS引擎+Cookie）
- 但在站点列表中显示B站分类站点
- 点击时提示"需要B站Cookie，暂不支持"

### 18.12 手机推送（Push）分析

`csp_Push` 站点：
- 作用：将网盘/浏览器链接推送到APP播放
- 需要Cloud-drive配置
- 支持夸克/阿里/百度网盘链接一键播放

**本项目策略**：
- v13.0 **实现基础推送功能**：
  - 分享面板：从其他APP分享到本APP
  - 剪贴板监听：检测到视频链接自动提示播放
  - 手动输入链接播放

### 18.13 短剧（Drama）分析

`csp_Djtt`（短剧天堂）等站点：
- 专门提供短剧内容（每集1-3分钟）
- 分类：女频/男频、虐恋/甜宠等

**本项目策略**：
- v13.0 **不单独实现**短剧分类
- 但如果采集站支持短剧分类，会自动显示

### 18.14 磁力（Magnet）分析

`csp_SixV`（新6V）等站点：
- 提供BT种子下载
- 支持磁力链接播放

**本项目策略**：
- v13.0 **实现磁力链接播放**：
  - 使用libtorrent或mxplayer外部播放
  - 支持分享磁力链接到APP

### 18.15 功能优先级总结

| 功能 | 优先级 | 说明 |
|------|--------|------|
| 影视采集站（type 0/1） | P0 | 核心功能，v13.0必须实现 |
| 直播源（lives[]） | P0 | 核心功能，v13.0必须实现 |
| 搜索 | P0 | 核心功能，v13.0必须实现 |
| 收藏/历史记录 | P0 | 核心功能，v13.0必须实现 |
| 播放器（ExoPlayer） | P0 | 核心功能，v13.0必须实现 |
| 备用解析（type=0） | P1 | 播放页提供解析按钮，v13.0实现 |
| flags匹配 | P1 | 自动推荐解析器，v13.0实现 |
| 壁纸 | P1 | 首页背景，v13.0实现 |
| 手机推送（分享面板） | P1 | Android分享Intent，v13.0实现 |
| 磁力链接 | P2 | 外部播放，v13.1实现 |
| 仓库规则（rules） | P2 | m3u8直链替换，v13.1实现 |
| 网盘登录（Cloud-drive） | P3 | 需要WebView扫码，v14.0实现 |
| AList | P3 | 网盘聚合，v14.0实现 |
| B站内容 | P3 | 需要JS引擎，v14.0实现 |
| JAR插件（csp_） | P4 | 需要集成TVBox SDK，v15.0+实现 |
| spider jar | P4 | 增强jar，v15.0+实现 |
| DOH | P4 | DNS加密，暂不实现 |

---

## 十九、v13.0 新增功能详细设计

### 19.1 备用解析功能

**UI设计**：
- 在播放页面（MoviePlayerFragment）底部工具栏添加"解析"按钮
- 点击弹出解析器选择列表（Dialog/BottomSheet）
- 每个解析器显示名称和支持的flag

**实现方式**：
```java
// ParseParser.java - 解析器配置解析
class ParseConfig {
    String name;      // 解析器名称
    String url;       // 解析URL（含?url=占位符）
    int type;         // 0=客户端, 1=服务端, 2=PHP, 3=JSON
    List<String> flags; // 支持的播放标志
    Map<String, String> headers; // 额外请求头
}

// 从仓库JSON的parses[]解析
List<ParseConfig> parseParses(JsonArray parsesArray) {
    List<ParseConfig> result = new ArrayList<>();
    for (JsonElement el : parsesArray) {
        JsonObject p = el.getAsJsonObject();
        ParseConfig cfg = new ParseConfig();
        cfg.name = p.get("name").getAsString();
        cfg.url = p.get("url").getAsString();
        cfg.type = p.get("type").getAsInt();
        // 解析ext中的flag和header
        JsonObject ext = p.has("ext") ? p.getAsJsonObject("ext") : new JsonObject();
        if (ext.has("flag")) {
            JsonArray flags = ext.getAsJsonArray("flag");
            for (JsonElement f : flags) {
                cfg.flags.add(f.getAsString());
            }
        }
        if (ext.has("header")) {
            cfg.headers = parseHeaders(ext.get("header"));
        }
        // 只保留type=0的客户端解析
        if (cfg.type == 0) {
            result.add(cfg);
        }
    }
    return result;
}

// 生成解析URL
String buildParseUrl(ParseConfig parse, String videoUrl, String flag) {
    // 如果parse.url包含flag匹配，则使用该解析
    if (parse.flags.contains(flag)) {
        return parse.url + videoUrl;
    }
    // 无flag匹配，使用默认解析
    return parse.url + videoUrl;
}
```

**解析流程**：
1. 用户点击"解析"按钮
2. 弹出解析器列表
3. 用户选择解析器
4. WebView打开 `parse.url + videoUrl`
5. 解析器返回嵌入的播放器页面
6. 用户直接在WebView中观看

### 19.2 壁纸功能

**UI设计**：
- 首页（MovieHomeFragment）使用壁纸作为背景
- 长按首页空白区域或点击"设置"中的"更换壁纸"
- 壁纸保存到SharedPreferences

**实现方式**：
```java
// WallpaperManager.java
class WallpaperManager {
    private static final String KEY_WALLPAPER_URL = "wallpaper_url";
    
    // 从仓库加载壁纸
    void loadWallpaper(String wallpaperUrl) {
        prefs.edit().putString(KEY_WALLPAPER_URL, wallpaperUrl).apply();
    }
    
    // 获取当前壁纸
    String getWallpaperUrl() {
        return prefs.getString(KEY_WALLPAPER_URL, null);
    }
    
    // 更换壁纸
    void changeWallpaper(Context context) {
        // 调用随机壁纸API
        String apiUrl = "https://api.vvhan.com/api/wallpaper/acgjj";
        OkHttpUtil.get(apiUrl, null, 10000)
            .subscribe(response -> {
                // 解析返回的JSON获取图片URL
                JsonObject json = new Gson().fromJson(response.body().string(), JsonObject.class);
                String imgUrl = json.get("img").getAsString();
                loadWallpaper(imgUrl);
                // 更新首页背景
                updateHomeBackground(imgUrl);
            });
    }
}
```

### 19.3 手机推送功能

**实现方式**：
```java
// ShareReceiverActivity.java - 接收分享
public class ShareReceiverActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                // 分享文本（视频链接）
                String link = intent.getStringExtra(Intent.EXTRA_TEXT);
                handleVideoLink(link);
            } else if (type.startsWith("image/")) {
                // 分享图片
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            // 分享多个文件
        }
        
        finish();
    }
    
    private void handleVideoLink(String link) {
        // 检测链接类型
        if (link.contains(".m3u8") || link.contains(".mp4")) {
            // 直接播放
            startActivity(new Intent(this, MoviePlayerActivity.class)
                .putExtra("url", link));
        } else if (link.contains("pan.quark") || link.contains("pan.baidu")) {
            // 网盘链接，提示暂不支持
            Toast.makeText(this, "网盘链接暂不支持直接播放", Toast.LENGTH_SHORT).show();
        } else {
            // 未知链接，尝试播放
            startActivity(new Intent(this, MoviePlayerActivity.class)
                .putExtra("url", link));
        }
    }
}
```

**AndroidManifest.xml配置**：
```xml
<activity android:name=".ui.ShareReceiverActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.SEND" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:mimeType="text/plain" />
    </intent-filter>
    <intent-filter>
        <action android:name="android.intent.action.SEND" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:mimeType="video/*" />
    </intent-filter>
</activity>
```

### 19.4 收藏/历史记录增强

**新增字段**：
- 影片ID
- 站点名称
- 分类名称
- 集数信息
- 播放进度（秒）
- 时间戳

**实现方式**：
```java
// HistoryDao.java
@Entity(tableName = "history")
class HistoryItem {
    @PrimaryKey
    String id;              // 站点ID + 影片ID + 集数索引
    String movieId;         // 影片ID
    String movieTitle;      // 影片标题
    String siteName;        // 站点名称
    String playFrom;        // 线路名称
    String episodeName;     // 集数名称
    int playIndex;          // 集数索引
    long position;          // 播放进度（毫秒）
    long duration;          // 视频总时长（毫秒）
    long timestamp;         // 时间戳
}
```

### 19.5 设置页面新增功能

**新增设置项**：
1. **解析设置**：
   - 默认解析器选择
   - 解析器列表管理（添加/删除/排序）
   - 自定义解析器（URL + 名称）

2. **壁纸设置**：
   - 使用仓库壁纸
   - 使用随机壁纸
   - 手动设置壁纸URL
   - 更换壁纸按钮

3. **推送设置**：
   - 启用剪贴板监听
   - 启用分享面板
   - 自动检测视频链接

4. **高级设置**：
   - 显示所有站点（包括JAR插件，标注"暂不支持"）
   - Cloud-drive配置（预留）
   - AList服务器配置（预留）
   - 蜘蛛jar地址（显示但不加载）

---

## 二十、实现计划更新

### Phase 1: 数据层（P0）
- [x] 数据模型（已完成基础模型）
- [ ] 新增：ParseConfig（解析器配置）
- [ ] 新增：WallpaperManager（壁纸管理）
- [ ] 新增：ShareReceiver（分享接收）
- [ ] 完善：SiteInfo增加typeLabel字段（MV/LIVE/PLUGIN/PUSH）

### Phase 2: 影视模块（P0）
- [ ] 站点仓库管理（从JSON提取type 0/1站点）
- [ ] 分类浏览
- [ ] 影片列表+分页
- [ ] 搜索
- [ ] 详情页
- [ ] 播放页（含解析按钮）

### Phase 3: 直播模块（P0）
- [ ] 直播源管理
- [ ] 频道列表（按分组）
- [ ] 直播播放

### Phase 4: 播放器（P0）
- [ ] ExoPlayer封装
- [ ] 手势控制（亮度/音量/进度）
- [ ] 倍速播放
- [ ] 投屏（DLNA）

### Phase 5: 辅助功能（P1）
- [ ] 收藏管理
- [ ] 历史记录
- [ ] 备用解析（WebView打开jx页面）
- [ ] 首页壁纸
- [ ] 分享面板

### Phase 6: 设置（P1）
- [ ] 站点设置
- [ ] 播放器设置
- [ ] 解析设置
- [ ] 壁纸设置
- [ ] 推送设置
- [ ] 高级设置（预留）
