# 饭太硬 TV 数据抓取方案

## 一、问题背景

目标网站 `http://www.饭太硬.net/tv` 返回的不是 HTML 页面，而是经过 Base64 编码 + zlib 压缩的二进制数据。直接 `curl` 访问会收到 JFIF 图片头响应，无法直接获取内容。

## 二、核心技术原理

### 2.1 数据格式分析

该网站采用的数据传输方式是 **Base64 编码的 gzip/zlib 压缩 JSON 数据**。浏览器（如 TVBox 类应用）在接收到响应后，会执行以下三步解码：

```
原始二进制数据 → Base64 解码 → zlib 解压缩 → 明文 JSON
```

这就是为什么直接 `curl` 看到的是乱码/JFIF 图片头，而实际内容是结构化的 JSON 配置。

### 2.2 为什么这样做能成功

| 步骤 | 原因 |
|------|------|
| curl 下载 | 绕过浏览器渲染，直接获取原始响应数据 |
| Base64 解码 | 网站用 Base64 将二进制数据编码为文本，防直接查看 |
| zlib 解压缩 | 网站进一步用 zlib 压缩数据，减小体积 |
| 正则匹配 | 响应中可能混入图片头等干扰数据，用正则定位 JSON 内容 |
| JSON 解析 | 最终拿到结构化的站点配置数据 |

## 三、实现步骤

### 3.1 下载原始数据

```bash
curl -s "http://www.饭太硬.net/tv" -o /tmp/tv_data.bin
```

### 3.2 Python 解码脚本

```python
import base64, zlib, json, re

with open('/tmp/tv_data.bin', 'rb') as f:
    data = f.read()

text = data.decode('utf-8', errors='ignore')

# 1. 正则匹配 Base64 字符串（长度大于200的连续字符）
b64_pattern = r'([A-Za-z0-9+/]{200,}={0,2})'
matches = re.findall(b64_pattern, text)

# 2. 遍历匹配结果，找到包含 JSON 特征的内容
for match in matches:
    decoded = base64.b64decode(match)
    content = decoded.decode('utf-8', errors='ignore')
    if '"sites"' in content:
        # 3. 成功解码，输出 JSON
        print(content)
        break
```

### 3.3 JSON 解析

解码后的 JSON 结构如下：

```json
{
  "spider": "图片URL",
  "wallpaper": "壁纸URL",
  "sites": [
    {"key": "玩偶", "name": "👽玩偶哥哥┃4K弹幕", "api": "csp_WoGGGuard", ...},
    {"key": "厂长", "name": "📔厂长┃不卡", "api": "csp_NewCzGuard", ...},
    ...
  ],
  "live": [...],
  "rules": [...],
  "ads": [...]
}
```

### 3.4 站点数据提取

```python
import json

sites_data = json.loads(content)
for site in sites_data['sites']:
    print(f"{site['name']} | Key: {site['key']} | API: {site['api']}")
```

## 四、完整代码

完整解码脚本已保存至：`/workspace/tv_scrape.py`

## 五、输出结果

### 5.1 玩偶哥哥

- **名称**: 👽玩偶哥哥┃4K弹幕
- **Key**: 玩偶
- **API**: csp_WoGGGuard
- **搜索**: 支持快速搜索 + 普通搜索
- **云盘配置**: tvfan/Cloud-drive.txt

### 5.2 全部站点列表（共 48 个）

#### 影视类

| # | 名称 | Key | API | 搜索 |
|---|------|-----|-----|------|
| 1 | 去【太太太硬了】领取嘟嘟盘免费容量 | 点我切源 | csp_DouDouGuard | 否 |
| 2 | 🗂我的云盘┃我配置 | MDrive | csp_MyDriveGuard | 普 |
| 3 | 👽玩偶哥哥┃4K弹幕 | 玩偶 | csp_WoGGGuard | 快+普 |
| 4 | 🚀叨观荐影┃预告片 | YGP | csp_YGPGuard | 否 |
| 5 | 🌞光影┃不卡 | 光影 | csp_T4Guard | 快+普 |
| 6 | 👒原创┃不卡 | 原创 | csp_YCyzGuard | 快+普 |
| 7 | 📔厂长┃不卡 | 厂长 | csp_NewCzGuard | 快+普 |
| 8 | 🌟立播┃不卡 | 立播 | csp_LibvioGuard | 快+普 |
| 9 | 👀瓜子┃不卡 | 瓜子 | csp_AppgzGuard | 快+普 |
| 10 | 🍄比特┃不卡 | 比特 | csp_BttwooGuard | 快+普 |
| 11 | 🍓糯米┃秒播 | 糯米 | csp_NmyswvGuard | 快+普 |
| 12 | 💮文采┃秒播 | 文采 | csp_JpysGuard | 快+普 |
| 13 | 🧀奶酪┃秒播 | 奶酪 | csp_T4Guard | 快+普 |
| 14 | 📺热播┃多线 | 热播 | csp_AppTTGuard | 快+普 |
| 15 | 🌸茉莉┃多线 | 视界 | csp_AppSxGuard | 快+普 |
| 16 | 🦊播客┃多线 | 播客 | csp_AppSxGuard | 快+普 |
| 17 | 🐻剧圈┃多线 | 剧圈 | csp_AppSxGuard | 快+普 |
| 18 | 🥝荐片┃多线 | 荐片 | csp_JPJGuard | 快+普 |
| 19 | 🏝奥特┃多线 | 奥特 | csp_AueteGuard | 快+普 |
| 20 | 🧲新6V┃磁力 | 新6V | csp_SixVGuard | 快+普 |

#### 动漫类

| # | 名称 | Key | API | 搜索 |
|---|------|-----|-----|------|
| 21 | 🦉咕咕┃动漫 | 咕咕 | csp_AppSxGuard | 快+普 |
| 22 | 🚌巴士┃动漫 | Dm84 | csp_Dm84Guard | 快+普 |
| 23 | 🐾日本┃动漫 | Anime1 | csp_Anime1Guard | 快+普 |

#### 体育/直播类

| # | 名称 | Key | API | 搜索 |
|---|------|-----|-----|------|
| 24 | ⚽八八┃看球 | 看球 | csp_KanqiuGuard | 否 |
| 25 | 🏀多多┃看球 | 多多 | csp_DoubaoGuard | 否 |
| 26 | 🏐吃瓜┃看球 | 吃瓜 | csp_LiveGzGuard | 否 |
| 27 | 🎮一直播┃直播 | alllive | csp_AllliveGuard | 否 |
| 28 | 🎶明星┃MV | MTV | csp_BiliGuard | 否 |
| 29 | 🐯虎牙┃直播 | 虎牙js | drpy2.min.js | 普 |
| 30 | 🐟斗鱼┃直播 | 斗鱼js | drpy2.min.js | 普 |

#### 网盘搜索类

| # | 名称 | Key | API | 搜索 |
|---|------|-----|-----|------|
| 31 | 💡聚剧剧┃四盘 | seed | csp_SeedhubGuard | 快+普 |
| 32 | 🎈聚盘搜┃四盘 | ZPan | csp_S_zpsGuard | 普 |
| 33 | 🍄抠抠┃搜搜 | 抠搜 | csp_KkSsGuard | 快+普 |
| 34 | 🌈优汐┃搜搜 | UC | csp_UuSsGuard | 快+普 |
| 35 | 🐟盘她┃夸父 | YpanSo | csp_YpanSoGuard | 快+普 |
| 36 | 🐞盘他┃嘟嘟 | BpanSo | csp_BpanSoGuard | 快+普 |

#### 教育/其他类

| # | 名称 | Key | API | 搜索 |
|---|------|-----|-----|------|
| 37 | 🛴手机┃推送 | push_agent | csp_PushGuard | 否 |
| 38 | 🅱哔哔合集┃弹幕 | Bili | csp_BiliGuard | 普 |
| 39 | 🅱哔哔演唱会┃弹幕 | Biliych | csp_BiliGuard | 否 |
| 40 | 📚儿童┃启蒙 | dr_兔小贝 | drpy2.min.js | 否 |
| 41 | 📚少儿┃教育 | 少儿教育 | csp_BiliGuard | 否 |
| 42 | 📚小学┃课堂 | 小学课堂 | csp_BiliGuard | 否 |
| 43 | 📚初中┃课堂 | 初中课堂 | csp_BiliGuard | 否 |
| 44 | 📚高中┃课堂 | 高中教育 | csp_BiliGuard | 否 |
| 45 | 🎙️易听音乐┃带歌词 | MTV1 | csp_MusicGuard | 否 |
| 46 | 🎧有声┃小说 | 有声小说 | csp_Tingshu275Guard | 否 |
| 47 | 🚑急救┃教学 | Aid | csp_FirstAidGuard | 否 |
| 48 | 请勿信视频中任何广告 | cc | csp_XPathGuard | 普 |

### 5.3 IPTV 直播源

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

## 六、注意事项

1. 该数据源配置会定期更新，需要定期重新抓取
2. 部分站点的 `ext` 字段包含加密参数，需配合对应的 TVBox 插件使用
3. 直播源 URL 可能失效，需定期验证
4. 网站可能有反爬机制，频繁请求可能被封禁
