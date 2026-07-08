# 个人助手 TV - 项目方案

Feature Name: project-overview
Updated: 2026-07-07

## 项目概述

一个基于Android WebView的影视+直播+彩票客户端应用，目标Android 16。应用从TVBox仓库配置源（如饭太硬）获取站点列表，从站点CMS API获取分类和影片数据，通过EXO Player原生播放器播放视频。

**包名**: webapp.newcloud.lottery.movie
**版本**: 9.9 (Build 74)
**核心技术**: Android WebView + Gradle构建 + 纯前端JS/CSS/HTML

## 目录结构

```
workspace/
├── app_new/                  # WebView 混合应用源文件（BeanShell 环境）
│   ├── index.java            # BeanShell入口：WebView配置+JS接口注册
│   ├── ExoPlayerActivity.java # 原生EXO Player Activity（Java源文件）
│   ├── main.html             # 主UI模板（6个Tab页面）
│   ├── webapp.json           # 应用元数据配置
│   ├── favicon.png           # 应用图标
│   ├── .monkeycode/          # 项目规范文档
│   └── assets/               # 前端资源
│       ├── css/
│       │   ├── style.css
│       │   └── nc-ux.css
│       ├── js/               # 17个JS模块
│       └── player/           # ArtPlayer + HLS.js
│
└── app_android/              # Android Studio/Gradle 项目（可直接构建APK）
    ├── build.gradle          # 根构建配置
    ├── settings.gradle
    ├── gradle.properties
    ├── gradlew / gradlew.bat # Gradle Wrapper
    ├── gradle/wrapper/
    └── app/
        ├── build.gradle      # 模块配置 (compileSdk 34, minSdk 26)
        └── src/main/
            ├── AndroidManifest.xml
            ├── java/webapp/newcloud/lottery/movie/
            │   ├── MainActivity.java      # 替代 index.java
            │   └── ExoPlayerActivity.java # 原生播放器 Activity
            ├── res/               # Android资源
            └── assets/            # 前端资源副本
```

## 两套代码的关系

- **app_new/**: 原始 WebView 混合应用源文件，用于 BeanShell 构建工具打包
- **app_android/**: 标准 Android Studio 项目，用 Gradle 构建 APK
- **app_android/app/src/main/assets/** 是 app_new/ 中 assets/ 和 main.html 的副本
- **app_android/app/src/main/java/** 中的 Java 文件是对应 app_new/ 中 BeanShell 代码的标准 Android 实现

## 构建方式

### BeanShell 构建（原始方式）
使用 webapp.json + index.java + 前端资源，通过 BeanShell WebView 构建工具打包成 APK。

### Gradle 构建（新方式）
```bash
cd app_android
./gradlew assembleDebug    # 构建 debug APK
./gradlew assembleRelease  # 构建 release APK（需签名配置）
```

APK 输出位置: `app_android/app/build/outputs/apk/debug/app-debug.apk`

## 数据获取架构

### 数据流全景

```
用户在主页长按"主页"按钮 → 弹出仓库管理面板
    ↓ 输入仓库URL（如饭太硬 http://www.饭太硬.net/tv）
    ↓ fetch + 解码
TVBox配置JSON (含sites/lives/rules)
    ↓ 解析sites数组，筛选可用CMS站点
站点列表 (48个站点中筛选出可用CMS站点)
    ↓ 保存到IndexedDB siteConfigs表
    ↓ 自动弹出站点选择面板
用户选择一个CMS站点
    ↓ ac=list → 获取分类列表
分类列表 (type_id/type_name/type_pid)
    ↓ 用户选择分类 → ac=detail&t=分类ID
影片列表 (含vod_play_url、vod_pic等)
    ↓ 保存到IndexedDB movies表
    ↓ 渲染主页3列卡片网格
用户点击影片卡片
    ↓ movieById() → 解析集数 → openVideoModal()
播放模态框 (ArtPlayer原生video/EXO Player)
```

**核心原则**: 所有数据都从远程CMS API获取，无本地种子/兜底数据。首次使用必须从仓库管理添加配置源。

### 饭太硬仓库配置解码

**问题**: 饭太硬网站(http://www.饭太硬.net/tv)返回的不是纯JSON，而是：
1. 外层包裹JPEG图片头(JFIF)
2. 内部数据经过Base64编码
3. Base64解码后是zlib压缩的二进制数据
4. zlib解压缩后才是明文JSON

**解码流程** (在 `nc-repo.js` 的 `decodeFtyResponse` 函数中):

```javascript
function decodeFtyResponse(text) {
  // 1. 正则匹配Base64字符串(长度>200的连续字符)
  var b64Pattern = /([A-Za-z0-9+\/]{200,}={0,2})/;
  var match = text.match(b64Pattern);
  if (!match) return null;
  
  var b64Str = match[1];
  
  // 2. Base64解码
  var binary = atob(b64Str);
  var bytes = new Uint8Array(binary.length);
  for (var i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  
  // 3. zlib解压缩(先尝试RawInflateSync，再尝试DecompressSync)
  var result = '';
  var decompressor = new zlib.DecompressSync();
  var input = new zlib.RawInflateSync();
  try {
    var inflated = input.decompress(bytes);
    result = new TextDecoder('utf-8').decode(inflated);
  } catch(e) {
    try {
      result = new TextDecoder('utf-8').decode(decompressor.decompress(bytes));
    } catch(e2) {
      result = b64Str; // 如果zlib失败，尝试直接解析
    }
  }
  
  // 4. 验证JSON特征并解析
  if (result.indexOf('sites') >= 0 || result.indexOf('spider') >= 0) {
    return JSON.parse(result);
  }
  return null;
}
```

**备用解码策略** (在 `fetchWarehouseConfig` 函数中):

```javascript
window.fetchWarehouseConfig = function(url) {
  return fetch(url, {cache: 'no-store'}).then(function(r) {
    return r.text();
  }).then(function(text) {
    // 策略1: 直接JSON解析(普通TVBox配置)
    try {
      var data = JSON.parse(text);
      if (data.sites || data.urls || data.spider) {
        return data;
      }
    } catch(e) {}
    
    // 策略2: 饭太硬特殊解码(Base64+zlib+JPEG)
    var decoded = decodeFtyResponse(text);
    if (decoded) return decoded;
    
    // 策略3: 正则提取JSON片段
    var jsonMatch = text.match(/\{[\s\S]*"sites"[\s\S]*\}/);
    if (jsonMatch) {
      try { return JSON.parse(jsonMatch[0]); } catch(e) {}
    }
    
    throw '无法解析配置';
  });
};
```

### TVBox配置JSON结构

饭太硬解码后的JSON结构：

```json
{
  "spider": "https://zl.wpscdn.cn/.../spider.jpg",
  "wallpaper": "壁纸URL",
  "sites": [
    {
      "key": "厂长",
      "name": "📔厂长┃不卡",
      "type": 1,
      "api": "https://czzy-api.com/api.php/provide/vod",
      "searchable": 1,
      "quickSearch": 1,
      "changeable": 1,
      "playerType": 2,
      "timeout": 10
    },
    {
      "key": "玩偶",
      "name": "👽玩偶哥哥┃4K弹幕",
      "type": 3,
      "api": "csp_WoGGGuard",
      "timeout": 30,
      "searchable": 1,
      "quickSearch": 1,
      "ext": { "Cloud-drive": "tvfan/Cloud-drive.txt" }
    },
    {
      "key": "虎牙js",
      "name": "🐯虎牙┃直播",
      "type": 0,
      "api": "drpy2.min.js",
      "ext": "https://gh-proxy.com/.../虎牙.js"
    }
  ],
  "lives": [
    {
      "name": "直播",
      "type": 0,
      "url": "http://example.com/live.m3u"
    }
  ],
  "rules": ["嗅探规则..."],
  "ads": ["广告过滤规则..."]
}
```

**关键点**:
- `type=1`: 普通CMS接口(JSON/XML API)，可直接请求分类和影片
- `type=2`: XML格式API
- `type=3`: JS爬虫或需要Spider插件的站点(如csp_WoGGGuard)，无法直接在WebView中请求
- `api` 包含 `.js`: JavaScript动态爬虫，需要加载执行JS规则
- `api` 包含 `csp_`: 需要TVBox原生Spider插件，WebView无法直接调用
- `api` 包含 `/api.php/provide`: 标准CMS接口，可直接请求

### 站点类型识别与处理

**在 `nc-movie-engine.js` 的 `chooseUsableSite` 函数中**:

```javascript
function chooseUsableSite(sites){
  var arr=(sites||[]).filter(function(s){
    var api=String(s&&s.api||''),t=String(s&&s.type);
    // 只选择type=0/1/2或无type字段的站点
    // 排除: .js结尾(JS爬虫)、csp_开头的(Spider插件)、spider字段
    return s&&api.indexOf('http')===0 
        &&api.indexOf('.js')<0 
        &&api.indexOf('spider')<0 
        &&api.indexOf('csp_')<0
        &&(t==='0'||t==='1'||t==='undefined'||!s.type);
  });
  // 优先选择CMS接口(/provide/vod)，其次XML接口
  arr.sort(function(a,b){
    var aa=String(a.api),bb=String(b.api);
    return (bb.indexOf('provide/vod')>=0?2:0)-(aa.indexOf('provide/vod')>=0?2:0);
  });
  return arr[0]||null;
}
```

**可用的CMS站点举例**(从饭太硬48个站点中筛选):
- 厂长影视 (czzy-api.com)
- 瓜子影视 (api.gzzjapi.com)
- 比特影视 (api.ttsp.tv)
- 糯米影视 (yming9.com)
- 文采影视 (jpys.me)

**不可用的站点**(需要Spider插件或JS爬虫):
- 玩偶哥哥 (csp_WoGGGuard) - 需要云盘Spider
- 立播 (csp_LibvioGuard) - 需要Spider插件
- 虎牙直播 (drpy2.min.js) - 需要JS爬虫规则
- 哔哔合集 (csp_BiliGuard) - 需要BiliSpider

### CMS API请求规范

**分类接口**: `ac=list`
```
GET https://czzy-api.com/api.php/provide/vod?ac=list
响应:
{
  "code": 1,
  "msg": "success",
  "class": [
    { "type_id": "1", "type_name": "电影", "type_pid": "0" },
    { "type_id": "2", "type_name": "连续剧", "type_pid": "0" },
    ...
  ]
}
```

**影片列表接口**: `ac=detail&t=分类ID&pg=页码`
```
GET https://czzy-api.com/api.php/provide/vod?ac=detail&t=1&pg=1
响应:
{
  "code": 1,
  "msg": "success",
  "page": "1",
  "pagecount": 100,
  "limit": "20",
  "total": "2000",
  "list": [
    {
      "vod_id": "123",
      "vod_name": "电影名称",
      "type_id": "1",
      "type_name": "电影",
      "vod_pic": "https://.../poster.jpg",
      "vod_year": "2024",
      "vod_remarks": "HD",
      "vod_content": "简介...",
      "vod_play_url": "第1集$http://...#第2集$http://...",
      "vod_play_from": "线路1$$$线路2"
    }
  ],
  "class": [...]
}
```

**搜索接口**: `ac=detail&wd=关键词`
```
GET https://czzy-api.com/api.php/provide/vod?ac=detail&wd=流浪地球
```

**详情接口**: `ac=detail&ids=影片ID`
```
GET https://czzy-api.com/api.php/provide/vod?ac=detail&ids=123
```

### 直播源解析

**M3U格式解析** (在 `parseLiveText` 函数中):
```
#EXTM3U
#EXTINF:-1 group-title="央视" tvg-logo="http://...",CCTV1
http://live.example.com/cctv1.m3u8
#EXTINF:-1 group-title="卫视" tvg-logo="...",湖南卫视
http://live.example.com/hunan.m3u8
```

**TXT格式解析**:
```
央视,#http://live.example.com/cctv1.m3u8
卫视,#http://live.example.com/hunan.m3u8
```

**饭太硬直播源URL** (从 `lives` 数组获取):
```json
{
  "name": "直播",
  "type": 0,
  "url": "http://www.饭太硬.net/m3u/live.m3u"
}
```

## 已实现功能

### 1. 影视点播（主页）

**数据加载**
- [x] 自定义CMS配置源加载（从仓库管理配置）
- [x] JSON/XML双格式解析
- [x] 分类自动提取（type_pid=0为根分类）
- [x] IndexedDB持久化存储（sources/categories/movies）
- [x] 分类页推荐兜底逻辑（当前分类无数据时尝试推荐数据）
- [x] 分页加载更多
- [x] 标题滚动 ticker 动画
- [x] 饭太硬特殊解码（Base64+zlib+JPEG）
- [x] 仓库配置多策略解析（直接JSON/饭太硬解码/正则提取）
- [x] 站点类型识别（type=1/2可用，type=3/csp_/js不可用）
- [x] 直播源M3U/TXT格式解析
- [x] 直播频道分组显示

**播放功能**
- [x] EXO Player原生Activity集成（Scheme A）
- [x] JavaScript Bridge (`window.exoPlayer.play/playEpisodes`)
- [x] ArtPlayer插件支持（nc-player-plugin）
- [x] 集数列表选择
- [x] 多解析器切换（自动重试）
- [x] 播放进度保存（localStorage，7天过期）
- [x] 手势控制：左滑亮度/右滑音量/长按2倍速
- [x] 竖屏模拟横屏（nc-landscape-sim）
- [x] 错误叠加层显示

**搜索**
- [x] 关键字模糊搜索（本地MOVIE_DATA过滤）
- [x] 远程API搜索（ac=detail&wd=）
- [x] 搜索历史（localStorage）
- [x] 多源并发搜索（16/24/32/64线程可调）
- [x] 精确/模糊匹配切换
- [x] 列表/网格视图切换
- [x] 搜索结果按源分组展示

**收藏/历史**
- [x] 收藏功能（localStorage movie_favs/movie_fav_meta）
- [x] 观看历史（IndexedDB movieHistory）
- [x] 「我的」页面统计展示

**仓库/站点管理**
- [x] 仓库列表CRUD（warehouses表）
- [x] 仓库分类管理（warehouseCategories表）
- [x] 站点配置管理（siteConfigs表）
- [x] 长按主页按钮弹出仓库面板
- [x] 配置源URL历史记录（最多20条）
- [x] 配置导入/导出（JSON文件）
- [x] 左上角站点管理面板（双Tab：仓库站点/本地站点）
- [x] 仓库站点Tab显示从仓库获取的站点（2列网格）
- [x] 本地站点Tab支持手动添加CMS接口
- [x] 仓库条目长按复制URL
- [x] 仓库分类切换（左侧栏）
- [x] 添加仓库弹窗（名称+URL）

**UI特性**
- [x] TV大字体卡片布局
- [x] 分类横向滚动导航
- [x] 分类下拉网格选择
- [x] 骨架屏加载动画
- [x] 空状态引导（三步教程）
- [x] 错误状态+重试按钮
- [x] 来源名称显示（tvSourceName）
- [x] 解析器按钮面板

### 2. 电视直播

**数据解析**
- [x] M3U格式解析（group-title/loop-title）
- [x] TXT格式解析（name,url）
- [x] 频道分组显示
- [x] 频道列表展示

**数据存储**
- [x] IndexedDB liveChannels表
- [x] 按来源(fromSite)分组管理
- [x] 删除频道功能

**播放**
- [x] 直播频道点击播放
- [x] 调用EXO Player播放直播流

**UI**
- [x] 直播分类导航
- [x] 直播频道卡片
- [x] 添加/刷新按钮

### 3. 彩票模块

**支持的彩种（8种）**
- [x] 超级大乐透（前区35选5+后区12选2）
- [x] 双色球（红球33选6+蓝球16选1）
- [x] 七乐彩（30选7+特别号）
- [x] 福彩3D（三位数字）
- [x] 排列3（三位数字）
- [x] 排列5（五位数字）
- [x] 7星彩（七位数字）
- [x] 快乐8（80选20，选十玩法）

**功能**
- [x] 5种预测算法（热号频率/冷号补位/区间均衡/和值跨度/混合模型）
- [x] 每期5组预测（makeGroups，异步分片执行）
- [x] 未来5期预测列表
- [x] 预测详情展开/折叠
- [x] 预测正确率统计（优/良好/一般/差）
- [x] 历史开奖数据本地存储
- [x] 联网同步开奖（多数据源fallback）
  - 双色球: cwl.gov.cn -> 东方财富
  - 大乐透: sports.edu.cn -> 东方财富
- [x] APP模式同步（AndroidSync接口）
- [x] 倒计时显示（距下期开奖）
- [x] 奖级表展示
- [x] 手动录入开奖
- [x] 一键随机选号（1-99组）
- [x] 号码粘贴解析
- [x] Delta游戏币计算器（见下方详细说明）
- [x] 号码复制（navigator.clipboard + execCommand降级）
- [x] 完整历史数据展开

**数据结构**
- [x] DLT_HISTORY / SSQ_HISTORY（嵌入数组，压缩存储）
- [x] LOT_DEFAULT_HISTORY（qlc/fc3d/pl3/pl5/qxc/kl8默认数据）
- [x] LOTTERY_CONFIG（各彩种参数配置）

**Delta游戏币计算器（三角洲行动）**

用于计算三角洲行动游戏内商品的人民币价值。

**页面结构**（`main.html:143-200`）：
```
section#main-lottery → div.container → div#tabsViewport
  └── section.tab-content#tab-delta [active]
        └── div.dcard
              ├── div.dcard-h [标题+描述]
              └── div (flex-wrap:wrap; gap:16px)
                    ├── div (flex:2; min-width:280px) [输入区]
                    │     ├── div.dcat [💰 纯币换算]
                    │     │     ├── input#dpureCoin [纯币数量M]
                    │     │     └── input#dratio [比例1:?]
                    │     ├── div.dcat [🎯 子弹配置]
                    │     │     ├── input#dawm [AWM数量] + input#dawmP [单价]
                    │     │     └── input#dred [红弹数量] + input#dredP [单价]
                    │     ├── div.dcat [🛡️ 6级防具配置]
                    │     │     ├── input#dhelm [6级头盔数量] + input#dhelmP [单价]
                    │     │     └── input#darmor [6级护甲数量] + input#darmorP [单价]
                    │     └── div.dbtns [按钮组]
                    │           ├── button → deltaCalc() [计算总价]
                    │           ├── button → deltaReset() [重置]
                    │           └── button → deltaDemo() [示例数据]
                    └── div.dres (flex:1.5; min-width:250px) [结果区]
                          ├── span#drPure [纯币价值]
                          ├── span#drAWM [AWM子弹]
                          ├── span#drRed [红弹]
                          ├── span#drHelm [6级头盔]
                          ├── span#drArmor [6级护甲]
                          ├── span#drTotal [总计]
                          └── span#drDetail [计算明细]
```

**计算逻辑**（`nc-lottery.js:1382-1408`）：

```javascript
function deltaCalc() {
  // 读取输入值
  var pureCoin = parseFloat(document.getElementById('dpureCoin').value) || 0;
  var ratio = parseInt(document.getElementById('dratio').value) || 40;
  var awm = parseInt(document.getElementById('dawm').value) || 0;
  var awmP = parseFloat(document.getElementById('dawmP').value) || 0;
  var red = parseInt(document.getElementById('dred').value) || 0;
  var redP = parseFloat(document.getElementById('dredP').value) || 0;
  var helm = parseInt(document.getElementById('dhelm').value) || 0;
  var helmP = parseFloat(document.getElementById('dhelmP').value) || 0;
  var armor = parseInt(document.getElementById('darmor').value) || 0;
  var armorP = parseFloat(document.getElementById('darmorP').value) || 0;
  
  // 纯币换算: 1M=1,000,000游戏币, 比例ratio表示1元=ratio万游戏币
  var pureVal = (pureCoin * 1000000 / ratio / 10000).toFixed(2);
  
  // 物品价值 = 数量 × 单价
  var awmVal = (awm * awmP).toFixed(2);
  var redVal = (red * redP).toFixed(2);
  var helmVal = (helm * helmP).toFixed(2);
  var armorVal = (armor * armorP).toFixed(2);
  
  // 总计
  var total = (parseFloat(pureVal) + parseFloat(awmVal) + parseFloat(redVal) 
             + parseFloat(helmVal) + parseFloat(armorVal)).toFixed(2);
  
  // 更新结果面板
  document.getElementById('drPure').textContent = pureVal + ' 元';
  document.getElementById('drAWM').textContent = awmVal + ' 元';
  document.getElementById('drRed').textContent = redVal + ' 元';
  document.getElementById('drHelm').textContent = helmVal + ' 元';
  document.getElementById('drArmor').textContent = armorVal + ' 元';
  document.getElementById('drTotal').textContent = total + ' 元';
  document.getElementById('drDetail').textContent = '纯币:' + pureCoin + 'M(' + ratio + ':1) AWM:' + awm + '(' + awmP + '/发) 红弹:' + red + '(' + redP + '/发) 头盔:' + helm + '(' + helmP + '/个) 护甲:' + armor + '(' + armorP + '/个)';
}
```

**示例数据**（`deltaDemo()`）：
- 纯币: 100M，比例 40:1
- AWM: 10发 × 0.8元/发 = 8.00元
- 红弹: 100发 × 0.15元/发 = 15.00元
- 6级头盔: 5个 × 1.5元/个 = 7.50元
- 6级护甲: 5个 × 1.5元/个 = 7.50元
- 纯币价值: 100 × 1000000 / 40 / 10000 = 250.00元
- 总计: 250.00 + 8.00 + 15.00 + 7.50 + 7.50 = 288.00元

**重置**（`deltaReset()`）：清空所有输入和结果，恢复默认值。

### 4. 本地数据库

**IndexedDB架构（NewCloudDB v2）**
- [x] sources表（数据源：name/url/base）
- [x] categories表（分类：sourceId/name/typeId）
- [x] movies表（影片：sourceId/category/vodId/完整字段）
- [x] warehouses表（仓库：categoryId/name/url）
- [x] warehouseCategories表（仓库分类）
- [x] siteConfigs表（站点：warehouseId/api/type/searchable）
- [x] liveChannels表（直播：fromSite/name/url/group）
- [x] movieHistory表（播放历史）
- [x] favorites表（收藏）

**索引**
- [x] sources: url, base
- [x] categories: sourceId, sourceId_name(复合)
- [x] movies: sourceId, category, sourceId_category(复合), vodId, updateTime
- [x] warehouses: categoryId, url, name
- [x] siteConfigs: warehouseId, api, type, name
- [x] liveChannels: fromSite, group, name

### 5. Android 16兼容性修复（已完成）

**性能优化**
- [x] IndexedDB递归游标删除改为getAllKeys+批量delete
- [x] 彩票预测计算拆分为50轮/块，setTimeout分片
- [x] setInterval改为变量保存，页面切换时清理
- [x] 影片渲染分批处理（每批20条）
- [x] 移除void txt.offsetWidth强制重排
- [x] localStorage写入前检查5MB空间限制
- [x] 安装兜底逻辑超时保护（5秒）

**错误处理**
- [x] 所有游标操作添加.catch()
- [x] makeGroups改为异步Promise
- [x] normalizeEntry使用_predictSync同步降级
- [x] resolvePlayUrl video元素失败时清理

**内存管理**
- [x] 清除uncleared setInterval引用
- [x] 删除重复函数定义
- [x] gesture绑定允许重复（重置dataset）
- [x] video元素tryParser失败时cleanup

**竞态修复**
- [x] loadMovieList添加expectedCat闭包保护
- [x] setupVideoTimeUpdate改用addEventListener

**变量修复**
- [x] nc-search.js修复searchSettings/searchResults/currentSite未声明

## 未实现/待完善功能

### 1. EXO Player集成

**已完成**
- [x] 基础播放接口（play/playEpisodes）
- [x] JavaScript Bridge注册
- [x] 原生Activity布局

**待完善**
- [ ] 集数列表选择后调用EXO Player（当前只播放第一集）
- [ ] 播放进度同步回WebView
- [ ] 字幕支持
- [ ] 音轨切换
- [ ] 投屏功能（Cast）
- [ ] 离线下载
- [ ] 饭太硬zlib解压在WebView中可能不可用（需测试zlib.RawInflateSync可用性）

### 2. 直播功能

**待实现**
- [ ] M3U8直播流播放测试
- [ ] 直播EPG节目单
- [ ] 直播录制功能
- [ ] 直播源质量检测
- [ ] 直播频道收藏

### 3. 搜索功能

**待实现**
- [ ] 搜索结果直接播放（当前仅alert提示）
- [ ] 搜索建议/联想
- [ ] 搜索过滤（按分类/年份/地区）
- [ ] 搜索排序（按相关度/时间）

### 4. 数据同步

**待实现**
- [ ] 云端备份/恢复
- [ ] 多设备同步
- [ ] 增量更新检测
- [ ] 数据压缩传输

### 5. UI/UX

**待实现**
- [ ] 深色/浅色主题切换
- [ ] 字体大小调节
- [ ] 播放速度调节UI（当前仅手势）
- [ ] 画中画模式
- [ ] 锁屏功能
- [ ] 家长控制

### 6. 性能优化

**待完善**
- [ ] 图片懒加载（当前全部预加载）
- [ ] 虚拟滚动（长列表优化）
- [ ] Web Worker处理预测计算（当前setTimeout分片）
- [ ] Service Worker离线缓存
- [ ] 数据库查询优化（分页游标）

### 7. 稳定性

**待完善**
- [ ] 网络断开重连机制
- [ ] API超时重试策略
- [ ] IndexedDB容量监控
- [ ] 内存泄漏检测
- [ ] 崩溃日志收集

### 8. 仓库管理面板改进

**待实现**
- [ ] 仓库条目左滑删除
- [ ] 仓库地址完整宽度显示（当前已实现）
- [ ] 主页按钮user-select:none（防止系统复制菜单）

## 技术架构

### 分层架构

```
┌─────────────────────────────────────┐
│         Android Native Layer        │
│  MainActivity.java (标准Android)     │
│  ExoPlayerActivity.java             │
└──────────────┬──────────────────────┘
               │ addJavascriptInterface
┌──────────────▼──────────────────────┐
│         WebView Layer               │
│         main.html                   │
└──────────────┬──────────────────────┘
               │ DOM + JS
┌──────────────▼──────────────────────┐
│         JavaScript Layer            │
│  app.js (入口/TV UI)                │
│  nc-movie-engine.js (影视引擎)      │
│  nc-lottery.js (彩票)               │
│  nc-search.js (搜索)                │
│  nc-live.js (直播)                  │
│  nc-db.js (IndexedDB)               │
│  nc-player.js (手势)                │
│  nc-cache.js (缓存)                 │
│  nc-page.js (页面/兜底)             │
│  nc-ui.js (UI组件)                  │
│  nc-repo.js (仓库)                  │
│  nc-site-manage.js (站点)           │
│  exo-player-wrapper.js (EXO桥接)    │
└─────────────────────────────────────┘
```

### 数据流

```
用户操作 → main.html DOM事件 → app.js / nc-*.js → IndexedDB / localStorage / fetch API
                                                         ↓
                                             远程CMS API (自定义源)
```

### 脚本加载顺序

```
ffzy_seed.js → hls.light.min.js → artplayer.js → nc-player-plugin.js → nc-db.js → nc-lottery.js
→ nc-movie-engine.js → nc-ui.js → nc-cache.js → nc-repo.js → nc-catalog-search.js
→ nc-player.js → nc-transitions.js → nc-page.js → exo-player-wrapper.js → app.js
```

## 关键设计决策

1. **EXO Player接口**: `exoPlayer`（小写），通过`addJavascriptInterface`注册
2. **JavaScript调用**: `window.exoPlayer.play()` 和 `window.exoPlayer.playEpisodes()`
3. **BeanShell上下文**: index.java中`context`和`webview`为全局变量
4. **彩票预测**: 从同步改为异步Promise，使用`_predictSync`同步降级
5. **IndexedDB**: 版本2，无迁移策略，使用getAllKeys替代递归游标
6. **localStorage**: 5MB限制检查，关键数据使用IndexedDB
7. **饭太硬解码**: 三层策略（直接JSON→Base64+zlib→正则提取）
8. **站点筛选**: 只选择type=0/1/2且api以http开头的CMS站点
9. **仓库管理**: 左侧分类栏+右侧仓库列表，长按复制URL
10. **站点管理**: 左上角双Tab面板（仓库站点/本地站点）
11. **数据采集**: 已移除手动采集功能，数据通过仓库配置源自动加载

## 注意事项

- Android 16严格ANR检测，所有耗时操作必须分片或异步
- 彩票预测compute-intensive，必须使用setTimeout分片
- setInterval必须保存引用并在页面切换时清理
- IndexedDB批量操作避免递归游标（栈溢出风险）
- localStorage大对象序列化阻塞主线程，需检查空间
- nc-lottery.js在全局作用域（非IIFE），其他模块均为IIFE
- 重复函数定义会导致覆盖，需清理
- 饭太硬配置源需要Base64+zlib解码，WebView中zlib API可能不可用
- 部分站点需要TVBox Spider插件（csp_*），WebView无法直接调用
- 直播源URL可能失效，需定期验证
- 站点API可能有反爬机制，频繁请求可能被封禁
- 手动采集功能已移除，数据通过仓库配置源自动加载

## 页面结构与交互逻辑

### 一、整体架构

单页应用（SPA），`file://` WebView 模式运行，无后端服务。所有数据存储在 IndexedDB 和 localStorage 中，远程数据通过 fetch/NativeHttp 从 CMS API 获取。

**文件依赖加载顺序**（`main.html:346-358`）：

```
ffzy_seed.js → hls.light.min.js → artplayer.js → nc-player-plugin.js
→ nc-db.js → nc-lottery.js → nc-movie-engine.js → nc-ui.js → nc-cache.js
→ nc-repo.js → nc-catalog-search.js → nc-player.js → nc-transitions.js
→ nc-page.js → exo-player-wrapper.js → app.js
```

**核心模块职责**：

| 文件 | 职责 |
|------|------|
| `nc-db.js` | IndexedDB 封装，CRUD 操作 |
| `nc-movie-engine.js` | 影视数据加载、缓存、配置、播放 |
| `nc-ui.js` | 骨架屏、空状态、搜索历史、Tab 切换、下拉刷新 |
| `nc-repo.js` | 仓库管理面板（长按主页触发） |
| `nc-site-manage.js` | 站点管理面板（选择采集源） |
| `nc-catalog-search.js` | 分类导航、搜索、侧滑切换 |
| `nc-player.js` | 播放器手势（亮度/音量/倍速） |
| `nc-live.js` | 直播频道管理、M3U/TXT 解析 |
| `nc-lottery.js` | 彩票数据、预测算法、开奖同步、Delta计算器 |
| `nc-page.js` | 页面注册系统、分类页推荐兜底 |
| `app.js` | 核心入口：TV UI 覆盖、数据库查看器、配置导入导出 |

### 二、HTML 结构（`main.html`）

#### 2.1 顶层布局

```
app-shell (min-height: 100vh, padding-bottom: 74px)
  └── main-pages (max-width: 1100px, margin: 0 auto)
        ├── section.main-page#main-home [active]    → 主页（影视）
        ├── section.main-page#main-live              → 直播
        ├── section.main-page#main-search            → 搜索
        ├── section.main-page#main-lottery           → 彩票
        ├── section.main-page#main-library           → 书架（收藏/历史）
        ├── section.main-page#main-mine              → 我的
        └── nav.bottom-main-nav                      → 底部导航栏（5 Tab）
```

**页面切换机制**（`app.js:140-151`, `nc-ui.js:286-292`）：

`switchMainPage(page)` 函数遍历所有 `.main-page` 元素，通过 `data-main` 属性匹配来切换 `active` 类。同时切换底部导航按钮的高亮状态。

#### 2.2 Tab 1: 主页（影视）`#main-home`

**结构层级**（`main.html:13-86`）：

```
section#main-home (data-main="home")
  └── div.tv-page
        ├── div.tv-header          [sticky top:0, z-index:10]
        │     ├── button#tvSourceBtn   → 点击 showSitePanel()
        │     ├── div.tv-search-wrap
        │     │     └── div.tv-search-box
        │     │           input#tvSearchInput [readonly, onclick→goToSearchPage()]
        │     │           div#searchHistoryDropdown [隐藏的下拉历史]
        │     └── div.tv-header-actions
        │           ├── ⚡ 解析器 toggleParserPanel()
        │           ├── ▣ 窗口 showWindowManager()
        │           └── + 更多 showMoreMenu()
        │
        ├── div.tv-cat-bar         [分类导航栏]
        │     ├── div#tvCatScroll  [水平滚动分类按钮]
        │     └── button.tv-cat-toggle [☰ 展开下拉]
        │
        ├── div#tvCatDropdown      [分类下拉网格]
        │     └── div#tvCatGrid    [4列网格]
        │
        ├── div.tv-content         [内容区]
        │     ├── div.tv-section-header
        │     │     ├── span#tvSectionName  [当前分类名]
        │     │     ├── div#ncTitleTicker   [采集标题滚动]
        │     │     └── button.tv-section-more [全部/返回]
        │     ├── div#movieLoadStatus / #tvLoadStatus  [状态提示]
        │     ├── div#tvGrid        [3列网格卡片]
        │     └── div#tvLoadMoreWrap [加载更多]
        │
        ├── div#emptyGuide         [空状态引导]
        ├── div#skeletonGrid       [骨架屏 6 卡片]
        └── div#errorState         [错误状态 + 重试按钮]
```

**关键交互**：

- `tvSourceBtn` 点击 → `showSitePanel()` (`app.js:124`) 弹出站点选择面板
- `tvSearchInput` 点击 → `goToSearchPage()` (`app.js:664`) 跳转到搜索页
- 分类切换 → `tvSetCat(c)` (`app.js:93-110`) 从 IndexedDB 读取本地数据渲染
- 侧滑切换分类 → `nc-catalog-search.js:232-261` 触摸事件检测左右滑动
- 下拉刷新 → `nc-ui.js:159-203` 检测 touch Y 轴位移 >80px 触发 `movieRefresh()`

#### 2.3 Tab 2: 直播 `#main-live`

**结构层级**（`main.html:88-102`）：

```
section#main-live (data-main="live")
  └── div#livePageContainer
        └── div.live-page
              ├── div.live-header
              │     ├── h2 "电视直播"
              │     └── div.live-controls
              │           ├── button.live-add-btn → showAddLiveChannel()
              │           └── button.live-refresh-btn → refreshLiveChannels()
              ├── div#liveCategories   [分组按钮行]
              └── div#liveChannels     [频道列表]
```

**初始化**（`nc-live.js:12-42`）：`initLivePage()` 动态构建 DOM（如果 `#livePageContainer` 内没有 `.live-page`），然后从 IndexedDB 加载频道数据。

**数据流**：
1. `NCDB.getAllLiveChannels()` → 获取所有频道
2. `groupChannels()` → 按 `group` 字段分组
3. `renderLiveCategories()` → 渲染分组按钮
4. `renderLiveChannels()` → 渲染频道列表
5. 点击频道 → `playLiveChannel(id)` → 调用 ExoPlayerWrapper 或创建临时 video 元素

#### 2.4 Tab 3: 搜索 `#main-search`

**结构层级**（`main.html:103-120`）：

```
section#main-search (data-main="search")
  └── div#searchPageContainer
        └── div.search-container
              ├── div.search-header
              │     ├── input#searchInput
              │     └── div.search-toolbar
              │           ├── button.search-type-btn [精确/模糊]
              │           ├── button.search-select-all
              │           ├── button.search-select-none
              │           └── button.search-start-btn → startMultiSourceSearch()
              ├── div#searchResults
              └── div#searchStatus
```

**初始化**（`nc-search.js:17-34`）：`initSearchPage()` 动态注入上述 DOM 结构。

**搜索流程**（`nc-search.js:70-137`）：
1. 用户输入关键词 → 点击"开始搜索"
2. `executeSearch()` 获取选中站点和线程数
3. `searchWithConcurrency()` 并发控制：队列 + 活跃计数器
4. `searchBySource()` 对每个站点发起 fetch → `normalizeSearchResults()` 统一格式
5. `updateSearchProgress()` 实时更新进度
6. `renderSearchResults()` 显示结果，支持按源切换和列表/网格视图

#### 2.5 Tab 4: 彩票 `#main-lottery`

**结构层级**（`main.html:121-204`）：

```
section#main-lottery (data-main="lottery")
  └── div.container
        ├── div.nav-bar           [水平滚动按钮行，8+1个彩种]
        │     ├── button.nav-btn [data-tab="dlt/ssq/qlc/fc3d/pl3/pl5/qxc/kl8/delta"]
        │     └── button.delta   [三角洲计算器]
        └── div#tabsViewport      [横向滚动视口，scroll-snap-type: x mandatory]
              ├── section.tab-content#tab-dlt [active]
              ├── section.tab-content#tab-ssq
              ├── ... (其他6个彩种)
              └── section.tab-content#tab-delta [内嵌计算器表单]
```

**Tab 切换机制**（`nc-ui.js:234-283`）：
- `switchTab(tab)` → 平滑滚动到目标 tab + 设置 active
- `initTabSnap()` → 监听 scroll 事件 + IntersectionObserver 自动对齐
- 滚动停止后 120ms 自动吸附到最近的 tab

**彩票功能**（`nc-lottery.js`）：
- 内置历史数据：`DLT_HISTORY`（约80期）+ `SSQ_HISTORY`（约80期）
- 其他6种彩种使用 `LOT_DEFAULT_HISTORY`
- 5种预测算法：热号频率 / 冷号补位 / 区间均衡 / 和值跨度 / 混合模型
- `predict()` 函数：对每个算法进行 650-780 轮模拟评分，选择最优组合
- `makeGroups()` 异步生成5组预测，使用 `setTimeout(runChunk, 0)` 分片避免阻塞
- 倒计时：`updateCD()` 每2秒更新距离下次开奖的时间
- 联网同步：`fetchAPI()` 从东方财富网/体校网获取最新开奖

**Delta游戏币计算器**（`main.html:143-200`）：
- 纯币换算：输入数量和比例 → 计算价值（公式：`纯币M * 1000000 / ratio / 10000`）
- 子弹配置：AWM + 红弹各数量×单价
- 防具配置：6级头盔 + 6级护甲各数量×单价
- 实时结果显示面板（分项+总计+明细）
- 三个按钮：计算总价(deltaCalc)、重置(deltaReset)、示例数据(deltaDemo)

#### 2.6 Tab 5: 书架 `#main-library`

**结构层级**（`main.html:205-211`）：

```
section#main-library (data-main="library")
  └── div.library-page
        ├── div.lib-head  [标题 + 描述]
        ├── div.lib-tabs  [收藏 / 历史 切换按钮]
        └── div#libraryContent  [动态内容]
```

**渲染逻辑**（`nc-movie-engine.js:933-951`）：
- 收藏模式：从 `movie_favs` 和 `movie_fav_meta` 读取，匹配 `MOVIE_DATA` 或 `MOVIE_INDEX` 中的影片
- 历史模式：从 `movie_history` 读取（IndexedDB 优先）
- 数据持久化：收藏使用 `lsGet('movie_favs')` / `lsSet('movie_favs', fav)`

#### 2.7 Tab 6: 我的 `#main-mine`

**结构层级**（`main.html:212-238`）：

```
section#main-mine (data-main="mine")
  └── div.mine-page
        ├── div.mine-card  [头像 + 用户名]
        ├── div.mine-grid  [4格统计：收藏/历史/直播/彩种]
        ├── div.mine-list  [功能按钮列表，8个]
        ├── div.mine-section-title "设置中心"
        └── div.mine-list  [设置按钮列表，3个]
```

**功能按钮映射**：

| 按钮 | 回调函数 | 说明 |
|------|---------|------|
| 站点管理 | `showSitePanel()` | 打开站点选择面板 |
| 多源搜索 | `initSearchPage()` | 初始化搜索页 |
| 搜索设置 | `showSearchSettings()` | 线程数/精确匹配/视图 |
| 直播源管理 | `showLiveSourceManager()` | 添加/测试/删除直播源 |
| 彩票预测 | `switchMainPage('lottery')` | 切换到彩票页 |
| 我的收藏 | `switchMainPage('library')` | 切换到书架收藏 |
| 配置导入/导出 | `showImportExport()` | JSON 导入导出 |
| 清空影视历史/收藏 | `clearMovieData()` | 清空 IndexedDB + localStorage |
| 清理缓存 | `clearAllCache()` | 清理 parse_cache_* / play_progress_* |
| 导出配置 | `exportConfig()` | 导出到剪贴板 |
| 关于 | `showAbout()` | 版本号 + 更新日志 |

### 三、CSS 布局体系（`style.css` + `nc-ux.css`）

#### 3.1 全局布局

```css
/* style.css:3-7 */
body {
  background: #05070d;
  min-height: 100vh;
  overflow-x: hidden;
}
.app-shell {
  min-height: 100vh;
  padding-bottom: 74px;  /* 为底部导航留空间 */
  background: radial-gradient(...) + linear-gradient(...);
}
.main-page {
  display: none;  /* 默认隐藏 */
}
.main-page.active {
  display: block;  /* 激活时显示 */
}
```

#### 3.2 底部导航栏

```css
/* style.css:8-13 */
.bottom-main-nav {
  position: fixed;
  bottom: 0;
  height: 70px;
  backdrop-filter: blur(16px);
  display: flex;
  justify-content: space-around;
}
.main-nav-btn.active {
  color: #9b5cff;  /* 紫色高亮 */
}
```

#### 3.3 影视 Grid 布局

```css
/* style.css:313 */
.tv-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);  /* 3列 */
  gap: 12px;
}
.tv-card {
  border-radius: 12px;
  overflow: hidden;
}
.tv-poster {
  height: 170px;
  background-size: cover;
  background-position: center;
}
```

#### 3.4 彩票 Tabs 滚动

```css
/* style.css:68-70 */
.tabs-viewport {
  display: flex;
  scroll-snap-type: x mandatory;  /* 强制吸附 */
  scroll-behavior: smooth;
  overflow-x: auto;
}
.tab-content {
  flex: 0 0 100%;  /* 每个占满宽度 */
  scroll-snap-align: start;
}
```

#### 3.5 面板系统

```css
/* style.css:328-336 */
.tv-panel-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,.6);
  z-index: 70;
  display: none;
  align-items: flex-end;  /* 从底部弹出 */
}
.tv-panel-overlay.show {
  display: flex;
}
.tv-panel {
  width: 100%;
  max-width: 600px;
  border-radius: 22px 22px 0 0;
  max-height: 75vh;
  display: flex;
  flex-direction: column;
}
```

#### 3.6 响应式断点

```css
/* style.css:271, 372-377 */
@media (max-width: 768px) {
  .tv-cat-grid { grid-template-columns: repeat(3, 1fr); }  /* 4列→3列 */
  .tv-poster { height: 150px; }
}
@media (min-width: 420px) {
  .video-player-wrap { height: 46vh; min-height: 260px; }
}
```

#### 3.7 UX 增强动画（`nc-ux.css`）

| 动画 | 文件:行号 | 效果 |
|------|----------|------|
| `ncPageIn` | `nc-ux.css:4-9` | 页面淡入+上移 |
| `skeleton-shimmer` | `style.css:193` | 骨架屏闪烁 |
| `ncSlideLeft/Right` | `nc-ux.css:348-355` | 分类切换滑入 |
| `ncTitleRoll` | `nc-ux.css:88-93` | 采集标题滚动 |
| `pulse` | `style.css:163-164` | 倒计时脉冲 |

#### 3.8 播放器全屏

```css
/* nc-ux.css:186-286 */
.video-modal.nc-video-fullscreen {
  /* 整个弹窗变全屏 */
}
.video-modal.nc-video-fullscreen.nc-landscape-sim {
  /* 横屏模拟：旋转90deg定位 */
}
```

#### 3.9 样式变量体系

```css
/* style.css:167 */
:root {
  --tv-primary: #2196F3;       /* 主色：蓝色 */
  --tv-secondary: #FF9800;     /* 辅色：橙色 */
  --tv-primary-dark: #1976D2;
  --tv-primary-light: #BBDEFB;
  --tv-secondary-dark: #F57C00;
  --tv_secondary-light: #FFE0B2;
}
```

**全局配色**：
- 背景：`#05070d` → `#0f1923` 渐变
- 卡片：`#1a2a3a` / `#101d2b`
- 文字：`#e0e0e0` / `#8899aa` / `#667788`
- 强调：`#9b5cff` (紫色) / `#4aa8ff` (蓝色) / `#ffa500` (橙色)

### 四、每个 Tab 页面的交互流程

#### 4.1 主页（影视）完整链路

```
用户打开 App
  │
  ├─→ switchMainPage('home') [默认激活]
  │
  ├─→ renderMovieHome() [app.js:24-67]
  │     ├─→ renderTvCats() [渲染分类按钮]
  │     ├─→ 从 MOVIE_DATA 过滤影片
  │     └─→ 分批渲染 tvGrid (每批20个，requestIdleCallback 风格)
  │
  ├─→ 用户点击分类按钮 → tvSetCat(c) [app.js:93-110]
  │     ├─→ 从 NCDB.getMovies(srcId, cat, 60) 读取本地数据
  │     ├─→ 更新 MOVIE_DATA
  │     └─→ renderMovieHome() 重新渲染
  │
  ├─→ 用户点击搜索框 → goToSearchPage() [app.js:664]
  │     └─→ switchMainPage('search') + initSearchPage()
  │
  ├─→ 用户点击影片卡片 → moviePlay(id) [nc-movie-engine.js:570-604]
  │     ├─→ movieById(id) 查找影片
  │     ├─→ getVodEpisodes(v) 解析集数
  │     ├─→ NCDB.saveHistory() 记录历史
  │     └─→ openVideoModal(v, eps) [nc-movie-engine.js:870-913]
  │           ├─→ 构建 episodeList 按钮行
  │           ├─→ buildParserBtns() 解析器选择
  │           ├─→ 自动播放第一集 playEpisodeByIndex(0)
  │           └─→ 尝试 EXO Player → 降级 ArtPlayer → 原生 video
  │
  └─→ 播放器手势 [nc-player.js]
        ├─→ 左半屏上滑 → 亮度调节
        ├─→ 右半屏上滑 → 音量调节
        ├─→ 长按 → 2倍速快进
        └─→ 全屏按钮 → nc-video-fullscreen + screen.orientation.lock('landscape')
```

#### 4.2 直播页交互流程

```
用户点击底部「直播」Tab
  │
  ├─→ switchMainPage('live')
  │
  ├─→ initLivePage() [nc-live.js:12-42]
  │     ├─→ 动态构建 DOM（如果尚未初始化）
  │     └─→ NCDB.getAllLiveChannels() 加载频道
  │
  ├─→ groupChannels() 按 group 分组
  │
  ├─→ renderLiveCategories() 渲染分组按钮
  │
  ├─→ renderLiveChannels() 渲染频道列表
  │
  ├─→ 用户点击分组 → switchLiveGroup(group)
  │     └─→ 过滤频道列表重新渲染
  │
  ├─→ 用户点击频道 → playLiveChannel(id)
  │     ├─→ ExoPlayerWrapper.play(url, name) [优先]
  │     └─→ 创建临时 video 元素 [降级]
  │
  └─→ 用户点击「添加」→ showAddLiveChannel()
        └─→ prompt 输入名称/URL/分组 → NCDB.saveLiveChannel()
```

#### 4.3 彩票页交互流程

```
用户点击底部「彩票」Tab
  │
  ├─→ switchMainPage('lottery')
  │
  ├─→ initLotteryPage() [app.js:625-627]
  │     ├─→ buildLotteryPage('dlt') + buildLotteryPage('ssq')
  │     └─→ fetchAPI('dlt') + fetchAPI('ssq') [联网获取最新开奖]
  │
  ├─→ 用户点击彩种按钮 → switchTab(tabId)
  │     ├─→ 设置 active 类
  │     └─→ 平滑滚动到对应 tab-content
  │
  ├─→ 每个彩种 Tab 内：
  │     ├─→ 倒计时卡片 [ccard]
  │     │     └─→ updateCD() 每2秒更新
  │     ├─→ 五组预测 [pcard]
  │     │     └─→ makeGroups() 异步生成5种算法预测
  │     ├─→ 统计面板 [irow]
  │     │     └─→ 均命中数 / 最佳命中 / 预测记录数
  │     ├─→ 未来5期预测表格
  │     ├─→ 预测历史与正确率 [acard]
  │     ├─→ 最近10期开奖表格
  │     ├─→ 一键随机选号
  │     └─→ 手动录入开奖
  │
  └─→ 用户点击"同步开奖" → fetchAPI(k)
        ├─→ 尝试中国体彩/福彩官网 API
        ├─→ 失败则回退到东方财富网
        └─→ handleLatest() 更新历史 + 判奖
```

#### 4.4 书架页交互流程

```
用户点击底部「历史/收藏」Tab
  │
  ├─→ switchMainPage('library')
  │
  ├─→ renderLibrary() [nc-movie-engine.js:934-951]
  │     ├─→ 读取 localStorage.getItem('library_tab') 确定当前标签
  │     ├─→ 收藏模式：lsGet('movie_favs') + lsGet('movie_fav_meta')
  │     └─→ 历史模式：lsGet('movie_history') 或 NCDB.getHistory()
  │
  └─→ 用户点击「收藏」/「历史」切换 → switchLibraryTab(tab)
        └─→ localStorage.setItem('library_tab', tab) + renderLibrary()
```

#### 4.5 「我的」页交互流程

```
用户点击底部「我的」Tab
  │
  ├─→ switchMainPage('mine')
  │
  ├─→ renderMine() [nc-movie-engine.js:952-965]
  │     ├─→ 更新收藏数 / 历史数 / 直播数
  │     └─→ 填充配置输入框
  │
  └─→ 用户点击功能按钮 → 对应处理函数
        ├─→ 站点管理 → showSitePanel() → nc-site-manage.js
        ├─→ 仓库管理 → 长按主页按钮触发 → nc-repo.js
        ├─→ 数据库查看 → showDbViewer() → app.js:441-586
        ├─→ 搜索设置 → showSearchSettings() → app.js:668-693
        └─→ 配置导入导出 → showImportExport() → app.js:731-774
```

### 五、全局交互

#### 5.1 Tab 切换逻辑

**主 Tab 切换**（`nc-ui.js:286-292`）：

```javascript
function switchMainPage(page) {
  // 遍历所有 .main-page，根据 data-main 匹配切换 active
  document.querySelectorAll('.main-page').forEach(el => {
    el.classList.toggle('active', el.getAttribute('data-main') === page);
  });
  // 同步底部导航按钮
  document.querySelectorAll('.main-nav-btn').forEach(el => {
    el.classList.toggle('active', el.getAttribute('data-main') === page);
  });
  // 页面特定初始化
  if (page === 'library') renderLibrary();
  if (page === 'mine') renderMine();
  window.scrollTo({ top: 0, behavior: 'smooth' });
}
```

**底部导航按钮**（`main.html:240-246`）：5个按钮，`data-main` 属性对应页面标识。

#### 5.2 长按主页按钮 → 仓库面板

**触发机制**（`app.js:133-153`）：

```javascript
// 监听 touchstart/touchend/mousedown/mouseup
btn.addEventListener('touchstart', () => startHomeLongPress(), { passive: true });
btn.addEventListener('touchend', () => {
  endHomeLongPress();
  if (_lpTriggered) {
    // 长按触发 → 显示仓库面板
    showRepoPanel();
    return;
  }
  // 短按 → 切换主页
  switchMainPage('home');
});
```

- 600ms 超时判定为长按
- 长按触发后阻止默认 click 事件
- 短按正常切换页面

#### 5.3 侧滑切换分类（主页）

**触摸手势**（`nc-catalog-search.js:232-261`）：

```
touchstart → 记录 startX, startY
touchmove  → 计算 dx, dy
           → 水平滑动 > 90px 且 |dx| > |dy| * 1.8 → 切换分类
           → animateContent() 播放滑入动画
```

#### 5.4 下拉刷新

**触摸手势**（`nc-ui.js:159-203`）：

```
touchstart → pullEligible = (scrollY <= 0 && 不在控制区域)
touchmove  → dy > 80px → pulling = true → 显示"释放刷新"
touchend   → pulling = true → movieRefresh() → 隐藏指示器
```

#### 5.5 播放器手势

**亮度/音量/倍速**（`nc-player.js:92-159`）：

```
touchstart on videoPlayerWrap:
  ├─ 点击控制区域 → 忽略手势
  ├─ 记录 startX (左半屏/右半屏)
  ├─ 启动 450ms 长按计时器
  └─ 记录 baseVolume, oldRate

touchmove:
  ├─ dy > 18px 且 dy > dx * 1.25 → gestureMode = 'vertical'
  ├─ 左半屏 → brightness = max(0, min(0.7, brightness - delta * 0.08))
  ├─ 右半屏 → volume = baseVolume + delta * 0.08
  └─ 清除长按计时器（移动即取消长按）

touchend / touchcancel:
  ├─ 如果长按触发 → playbackRate = 2 + showTip('2倍速快进')
  └─ 恢复原速度
```

### 六、弹窗/面板系统

#### 6.1 面板通用结构

所有面板遵循统一模式（`app.js:122-127`）：

```javascript
function showXxxPanel() {
  var el = document.getElementById('xxxOverlay');
  el.style.display = 'flex';
  setTimeout(() => el.classList.add('show'), 10);  // 触发动画
}
function hideXxxPanel() {
  var el = document.getElementById('xxxOverlay');
  el.classList.remove('show');
  setTimeout(() => el.style.display = 'none', 250);  // 等待动画结束
}
```

#### 6.2 仓库管理面板 `#repoPanelOverlay`

**触发**：长按主页按钮 (`app.js:134`) 或 `showRepoPanel()`

**结构**（`main.html:248-263`）：

```
tv-panel-overlay#repoPanelOverlay
  └── tv-panel.tv-repo-panel
        ├── tv-panel-header [刷新按钮 | 标题"仓库管理" | 添加按钮]
        └── tv-panel-body.tv-repo-body
              ├── tv-repo-sidebar#repoSidebar    [分类列表]
              └── tv-repo-content#repoContent    [仓库列表]
```

**交互**（`nc-repo.js`）：
1. 获取分类 → `getWarehouseCategories()` → 渲染 sidebar
2. 选择分类 → `selectWarehouseCategory(id)` → 渲染该分类下的仓库
3. 点击仓库 → `selectWarehouse(id, url)` → fetch 配置 → 解析站点 → 保存 IndexedDB → 打开站点面板
4. 添加仓库 → 输入名称/URL → `detectUrlType()` 智能检测 → `saveWarehouse()`

#### 6.3 站点选择面板 `#sitePanelOverlay`

**触发**：`showSitePanel()` (`app.js:124`) 或仓库选择后自动打开

**结构**（`main.html:266-280`）：

```
tv-panel-overlay#sitePanelOverlay
  └── tv-panel.tv-site-panel
        ├── tv-panel-header [标题"选择采集源"]
        └── tv-panel-body
              └── tv-site-grid#siteGrid  [站点列表]
```

**交互**（`nc-site-manage.js`）：
- 显示两类站点：warehouse（仓库获取）+ local（手动添加）
- 点击站点 → `selectSite(id)` → 获取分类 → `onCategoriesLoaded()` → 更新 `movieConfig.site` → 切换推荐页

#### 6.4 更多菜单面板 `#morePanelOverlay`

**触发**：首页右上角 "+" 按钮 (`main.html:32`)

**内容**（`main.html:283-313`）：
- 配置源 → 输入 URL → 加载
- 扫码 → 读取图片二维码
- 预设源 → 从 `PRESET_SOURCES` 列表选择
- 导入导出 → JSON 配置

#### 6.5 数据库查看器 `#dbViewerOverlay`

**触发**：`showDbViewer()` (`app.js:441`)

**结构**（`main.html:413-437`）：
- 源选择下拉框 + 分类下拉框
- 数据列表 + 分页导航

**交互**（`app.js:497-569`）：
- 选择源 → 加载该源的分类
- 选择分类 → `dbViewFetch()` 查询数据
- 不分源时聚合所有源的数据
- 分页：每页 30 条

#### 6.6 搜索设置弹窗 `#searchSettingsOverlay`

**触发**：`showSearchSettings()` (`app.js:668`)

**内容**（`main.html:440-472`）：
- 并发线程数：16/24/32/64（单选按钮）
- 精确匹配开关
- 结果视图：列表/网格（单选按钮）

**存储**：`localStorage.setItem('search_settings', JSON.stringify(settings))`

#### 6.7 直播源管理弹窗 `#liveSourceOverlay`

**触发**：`showLiveSourceManager()` (`app.js:696`)

**内容**（`main.html:475-486`）：
- 直播源列表（名称/分组/URL预览）
- 测试按钮 + 删除按钮

#### 6.8 视频播放模态框 `#videoModal`

**触发**：`openVideoModal(v, eps)` (`nc-movie-engine.js:870`)

**结构**（`main.html:315-344`）：
- 视频播放器区域（ArtPlayer 挂载点 + 原生 video）
- 时间显示 + 全屏/倍速控制
- 解析器选择行
- 选集列表
- 错误覆盖层

### 七、数据流

#### 7.1 IndexedDB 读写（`nc-db.js`）

**数据库结构**：

| 表名 | 字段 | 说明 |
|------|------|------|
| sources | id, name, url, base_url | 数据源 |
| categories | id, source_id, type_id, type_name, type_pid | 分类 |
| movies | id, source_id, category, title, pic, play, ... | 影片 |
| warehouses | id, category_id, name, url | 仓库配置 |
| warehouse_categories | id, name | 仓库分类 |
| site_configs | id, warehouse_id, name, api, type, searchable, ... | 站点配置 |
| live_channels | id, group, name, url, source_id | 直播频道 |
| history | id, movie_id, title, type, pic, timestamp | 观看历史 |
| favorites | id, movie_id, title, type, pic | 收藏 |

**关键 API**：
```javascript
NCDB.init()                    // 初始化数据库
NCDB.saveSource(name, url, base)  // 保存数据源
NCDB.saveCategories(srcId, classes) // 保存分类
NCDB.saveMovies(srcId, cat, items)  // 批量保存影片
NCDB.getMovies(srcId, cat, limit)   // 查询影片
NCDB.getSources()                // 获取所有源
NCDB.clearSource(srcId)          // 清空源数据
```

#### 7.2 localStorage 使用

| Key | 类型 | 说明 |
|-----|------|------|
| `movie_config_url` | string | 上次使用的配置源 URL |
| `movie_config_history` | array[] | 配置历史（最多20条） |
| `movie_favs` | array[string] | 收藏影片 ID 列表 |
| `movie_fav_meta` | array[object] | 收藏元数据（标题/类型/图片） |
| `movie_history` | array[object] | 观看历史 |
| `movie_search_history` | array[string] | 搜索历史（最多20条） |
| `library_tab` | string | 书架当前标签 ('fav'/'history') |
| `search_settings` | object | 搜索设置（线程数/精确匹配/视图） |
| `nc_cms_*` | object | CMS 缓存（含 time + data） |
| `parse_cache_*` | string | 解析器缓存 URL |
| `play_progress_*` | object | 播放进度（t + timestamp） |

#### 7.3 远程 API 调用

**影视 CMS 接口**：
```
GET {api}?ac=detail          → 首页推荐数据（含 class + list）
GET {api}?ac=list            → 分类列表
GET {api}?ac=detail&t={tid}  → 分类影片列表
GET {api}?ac=detail&wd={kw}  → 搜索
GET {api}?ac=detail&id={id}  → 影片详情
GET {api}?ac=detail&pg={n}&t={tid} → 分页列表
```

**直播源 M3U/TXT 解析**：
- M3U: 解析 `#EXTINF` 行提取频道名、分组、logo
- TXT: 解析 `频道名,URL` 格式

**彩票联网同步**：
- 中国体彩网: `fetchShSportsDltDirect()`
- 中国福彩网: `fetchSsqDirect()`
- 东方财富网: `fetchEastmoneyDirect()` (回退源)

#### 7.4 数据加载流程

```
用户选择站点/分类
  │
  ├─→ 检查 IndexedDB 缓存 → NCDB.getMovies(srcId, cat, 60)
  │     └─→ 有数据 → 直接渲染
  │
  ├─→ 检查 localStorage CMS 缓存 → ncReadCmsCache(base, cat, page)
  │     └─→ 有数据 → 显示缓存 + 后台刷新
  │
  ├─→ fetch API → parseVodListData() → normalizeVod()
  │     ├─→ 写入 IndexedDB → NCDB.saveMovies()
  │     └─→ 写入 localStorage 缓存 → ncSaveCmsCache()
  │
  └─→ 渲染 tvGrid → 每批20个卡片 → requestIdleCallback 风格分批
```

### 八、关键函数调用链汇总

#### 8.1 影视加载主链路

```
renderMovieHome() [app.js:24]
  └─→ renderTvCats() [app.js:69]
        └─→ NCDB.getDistinctCategoryNames() [如果有数据库]
              └─→ 渲染分类按钮
  └─→ MOVIE_DATA.filter() [过滤当前分类]
        └─→ 分批渲染 tvGrid
              └─→ renderMine() [更新统计数字]
              └─→ hideEmptyGuide() / showEmptyGuide()
              └─→ updateLoadMoreBtn()
```

#### 8.2 播放主链路

```
moviePlay(id) [nc-movie-engine.js:570]
  └─→ movieById(id) [nc-movie-engine.js:152]
        └─→ MOVIE_DATA / MOVIE_INDEX / listCache 三级查找
  └─→ getVodEpisodes(v) [nc-movie-engine.js:562]
        └─→ ffzyParsePlayUrl(v.raw) [解析 vod_play_url]
              └─→ 分离 direct (m3u8) 和 other 线路
  └─→ NCDB.saveHistory() [记录观看历史]
  └─→ openVideoModal(v, eps) [nc-movie-engine.js:870]
        └─→ EXOPlayer.playEpisodes() [如果可用]
              └─→ 否则 openVideoModal()
                    └─→ buildParserBtns() [解析器选择]
                    └─→ setupVideoTimeUpdate() [时间显示]
                    └─→ playEpisodeByIndex(0) [自动播放首集]
                          └─→ resolvePlayUrl() [解析器处理]
                                └─→ NCPlayerPlugin.mount() [ArtPlayer]
                                      └─→ video.play()
```

#### 8.3 仓库管理主链路

```
长按主页按钮 [app.js:133-153]
  └─→ showRepoPanel() [app.js:122]
        └─→ renderRepoPanel() [nc-repo.js:265]
              └─→ getWarehouseCategories() → 渲染 sidebar
              └─→ renderWarehousesForCategory() → 渲染仓库列表
   └─→ 用户点击仓库 → selectWarehouse(id, url) [nc-repo.js:369]
        └─→ fetchWarehouseConfig(url) [nc-repo.js:148]
              └─→ 尝试直接 JSON → 饭太硬 Base64+zlib 解码 → 正则提取
        └─→ parseSitesFromConfig(config) [nc-repo.js:201]
              └─→ 支持 sites/urls/drives 三种格式
        └─→ NCDB.saveSiteConfig() × N [保存每个站点]
        └─→ showSitePanel() [打开站点选择面板]
```

### 九、特殊处理与兼容性

#### 9.1 NativeHttp 桥接

多个模块检测 `window.NativeHttp && NativeHttp.httpGet`，用于 WebView 环境下的原生 HTTP 请求，绕过 CORS 限制。

#### 9.2 分批渲染避免阻塞

```javascript
// app.js:32-62: 每批20个卡片，使用 setTimeout 分片
var batchSize = 20;
function renderBatch() {
  var end = Math.min(batchIdx + batchSize, batchList.length);
  // ... 构建 HTML
  if (end >= batchList.length) {
    grid.innerHTML = batchList.map(...).join('');
  } else {
    setTimeout(renderBatch, 0);
  }
}
```

#### 9.3 彩票预测分片计算

```javascript
// nc-lottery.js:129-164: 每50轮一个 chunk，setTimeout 避免阻塞
function runChunk() {
  var end = Math.min(started + chunkSize, rounds);
  // ... 计算
  if (started < rounds) {
    setTimeout(runChunk, 0);
  } else {
    resolve({ f: bestF, b: bestB, ... });
  }
}
```

#### 9.4 页面切换清理定时器

```javascript
// nc-movie-engine.js:149
if (window._clearAppIntervals) window._clearAppIntervals();
```

切换 Tab 时清理彩票倒计时、同步等定时器，防止 Android 16 ANR。
