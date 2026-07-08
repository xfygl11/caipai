# 饭太硬 TV 站点深度分析报告

## 一、数据获取方式

### 1.1 数据源
- **URL**: http://www.饭太硬.net/tv
- **格式**: Base64 编码 + zlib 压缩的 JSON 数据，外层包裹在 JPEG 图片中
- **获取方式**: `curl` 下载后 Python 解码

### 1.2 Spider 插件
- **URL**: https://zl.wpscdn.cn/2026/07/04/space_img/dedbf9ed-e86e-4603-b194-5bd20df8eb4e.jpg
- **格式**: Android DEX 文件（JAR 包）
- **内容**: TVBox 爬虫插件，包含 44+ 个 Guard 类

## 二、站点分类统计

### 2.1 按内容类型分类

| 分类 | 数量 | 占比 |
|------|------|------|
| 影视点播 | 20 | 41.7% |
| 动漫 | 3 | 6.3% |
| 直播/体育 | 7 | 14.6% |
| 网盘/搜索 | 9 | 18.8% |
| 教育/其他 | 9 | 18.8% |
| **总计** | **48** | **100%** |

### 2.2 按 API 类型分组

#### csp_BiliGuard (7 个站点)
基于哔哩哔哩的爬虫，主要用于教育和弹幕内容：
- 🎶明星┃MV
- 🅱哔哔合集┃弹幕
- 🅱哔哔演唱会┃弹幕
- 📚少儿┃教育
- 📚小学┃课堂
- 📚初中┃课堂
- 📚高中┃课堂

#### csp_AppSxGuard (4 个站点)
多线路影视爬虫：
- 🌸茉莉┃多线
- 🦊播客┃多线
- 🐻剧圈┃多线
- 🦉咕咕┃动漫

#### csp_T4Guard (2 个站点)
T4 架构影视爬虫：
- 🌞光影┃不卡
- 🧀奶酪┃秒播

#### drpy2.min.js (3 个站点)
基于 JavaScript 的动态爬虫：
- 🐟斗鱼┃直播
- 🐯虎牙┃直播
- 📚儿童┃启蒙

#### 单站点 API (32 个)
每个 API 只对应一个站点，包括：
- csp_WoGGGuard: 玩偶哥哥
- csp_NewCzGuard: 厂长
- csp_AppgzGuard: 瓜子
- csp_SixVGuard: 新6V
- 等等...

## 三、重点站点详解

### 3.1 玩偶哥哥 (csp_WoGGGuard)

```json
{
  "key": "玩偶",
  "name": "👽玩偶哥哥┃4K弹幕",
  "type": 3,
  "api": "csp_WoGGGuard",
  "timeout": 30,
  "searchable": 1,
  "quickSearch": 1,
  "changeable": 0,
  "ext": {
    "Cloud-drive": "tvfan/Cloud-drive.txt"
  }
}
```

**特点**:
- 支持 4K 弹幕
- 支持快速搜索 + 普通搜索
- 需要云盘配置 (tvfan/Cloud-drive.txt)
- 超时时间 30 秒（较长，说明数据源响应慢）

### 3.2 厂长影视 (csp_NewCzGuard)

```json
{
  "key": "厂长",
  "name": "📔厂长┃不卡",
  "api": "csp_NewCzGuard",
  "timeout": 10,
  "searchable": 1,
  "quickSearch": 1,
  "changeable": 1,
  "playerType": 2
}
```

**特点**:
- 不卡播放
- 支持换源 (changeable: 1)
- 播放器类型 2

### 3.3 立播 (csp_LibvioGuard)

```json
{
  "key": "立播",
  "name": "🌟立播┃不卡",
  "api": "csp_LibvioGuard",
  "timeout": 10,
  "searchable": 1,
  "quickSearch": 1,
  "changeable": 1,
  "ext": {
    "Cloud-drive": "tvfan/Cloud-drive.txt",
    "siteUrl": "https://www.libvio.pw/"
  }
}
```

**特点**:
- 明确指定了站点地址: https://www.libvio.pw/
- 支持云盘配置
- 支持换源

## 四、Spider JAR 分析

### 4.1 文件结构

```
spider.jar
├── classes.dex          (36,724 bytes) - Android DEX 文件
├── META-INF/MANIFEST.MF - JAR 清单
└── assets/
    ├── ftyguard_v7.so   (81,520 bytes) - ARM32 原生库
    ├── ftyguard_v8.so   (102,440 bytes) - ARM64 原生库
    └── ftyshinidie.guard (960,181 bytes) - 加密/混淆的 DEX 文件
```

### 4.2 包含的 Guard 类 (44 个)

从 DEX 文件中提取的类名：

**基础类**:
- BaseSpiderGuard

**影视类**:
- T4Guard, JPJGuard, NewCzGuard, YCyzGuard, LibvioGuard
- AppgzGuard, BttwooGuard, NmyswvGuard, JpysGuard, AueteGuard
- SixVGuard, AppTTGuard, AppSxGuard, AppSKGuard, AppNoxGuard

**动漫类**:
- Dm84Guard, Anime1Guard

**搜索/网盘类**:
- WoGGGuard, KkSsGuard, UuSsGuard, YpanSoGuard, BpanSoGuard
- SeedhubGuard, S_zpsGuard, MyDriveGuard, PushGuard, WebDAVGuard

**直播/体育类**:
- KanqiuGuard, DoubaoGuard, LiveGzGuard, AllliveGuard

**教育/其他类**:
- BiliGuard, MusicGuard, YGPGuard, FirstAidGuard, Tingshu275Guard
- YoutubeGuard

**特殊类**:
- KaFeiGuard (咖啡)
- Sir88Guard (Sir 搜)
- LocalGuard (本地)
- AppYsV2Guard (APP 接口 V2)

### 4.3 混淆结构

存在 mergeguard 混淆包，包含多个混淆类：
- OoOoO0O0o0oOoO0O
- oOoOoOoOoOoOoO0o
- 等 20+ 个混淆类

## 五、搜索能力统计

| 搜索类型 | 数量 | 站点示例 |
|----------|------|----------|
| 快速+普通 | 28 | 玩偶、厂长、瓜子、比特等 |
| 仅普通 | 10 | MDrive、ZPan、虎牙、斗鱼等 |
| 不可搜索 | 10 | 看球、听歌、教育等 |

## 六、云盘配置

以下站点需要云盘配置 (tvfan/Cloud-drive.txt):
1. MDrive (我的云盘)
2. 玩偶哥哥
3. 立播
4. 抠抠搜搜
5. 优汐搜搜
6. 盘她夸父
7. 盘他嘟嘟
8. 手机推送

## 七、外部依赖

### 7.1 JSON 配置 URL
- 明星 MV: https://nos.netease.com/ysf/5af5fbe12a88b7c45aa1c21e6551826c.txt
- 哔哔合集: https://nos.netease.com/ysf/0075389dca9afadd4614e9713765ff17.txt
- 哔哔演唱会: https://nos.netease.com/ysf/6496356286589c68f52c2f99c0c674c7.txt
- 少儿教育: https://nos.netease.com/ysf/89370c8ddf36b5e1beb4d71adb921bda.txt
- 小学课堂: https://nos.netease.com/ysf/d7a21cf34ede56f5c686ecfba5fc7e3f.txt
- 初中课堂: https://nos.netease.com/ysf/8f55d520f8d70056695740ef151744a7.txt
- 高中课堂: https://nos.netease.com/ysf/c66a4b5356141c49fd45ec51568017b4.txt

### 7.2 JavaScript 爬虫 URL
- 虎牙直播: https://gh-proxy.com/https://raw.githubusercontent.com/fantaiying7/EXT/refs/heads/main/虎牙.js
- 斗鱼直播: https://git.yylx.win/https://raw.githubusercontent.com/fantaiying7/EXT/refs/heads/main/斗鱼直播.js
- 儿童启蒙: https://git.yylx.win/https://raw.githubusercontent.com/fantaiying7/EXT/refs/heads/main/兔小贝.js

### 7.3 站点地址
- 立播: https://www.libvio.pw/
- 新6V: https://www.xb6v.com/
- 聚盘搜: http://107.173.211.148/

## 八、IPTV 直播源

| 名称 | URL |
|------|-----|
| develop202 | https://gh.927223.xyz/https://raw.githubusercontent.com/develop202/migu_video/refs/heads/main/interface.txt |
| Kimentanm | https://gh.927223.xyz/https://raw.githubusercontent.com/Kimentanm/aptv/master/m3u/iptv.m3u |
| 范明明（需开启V6网络） | https://nos.netease.com/ysf/3d75a78a0fc7ede372c03598d6d10367.m3u |
| 世界杯 | http://82.156.243.185:33389/fwc.m3u |
| 虎牙一起看 | https://sub.ottiptv.cc/huyayqk.m3u |
| 斗鱼一起看 | https://sub.ottiptv.cc/douyuyqk.m3u |
| B站直播 | https://sub.ottiptv.cc/bililive.m3u |
| YY轮播 | https://sub.ottiptv.cc/yylunbo.m3u |

## 九、技术总结

### 9.1 数据流向
```
饭太硬网站 (Base64+zlib+JPEG)
    ↓ 下载
原始二进制数据
    ↓ Base64 解码
压缩 JSON
    ↓ zlib 解压缩
明文 JSON 配置
    ↓ 解析
站点列表 + Spider JAR URL
    ↓ 下载 JAR
Android DEX 文件
    ↓ 反编译
44+ 个爬虫类
```

### 9.2 关键技术点
1. **IDN 域名处理**: 中文域名需要特殊处理（Python urllib 不支持，需用 curl）
2. **多层编码**: Base64 + zlib + JPEG 包裹
3. **Android DEX**: Spider 插件是 Android 格式的 DEX 文件
4. **原生库保护**: 使用 ARM32/ARM64 SO 文件进行加密保护
5. **代码混淆**: mergeguard 包使用混淆类名

### 9.3 限制说明
- Spider JAR 中的核心逻辑被加密/混淆，无法直接读取
- 需要 TVBox 或兼容应用才能运行这些爬虫
- 部分 ext 字段包含加密参数，需要对应插件解密
