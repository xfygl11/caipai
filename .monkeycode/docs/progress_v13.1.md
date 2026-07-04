# 开发进度记录

## v13.1 - 首页壁纸 + 分享面板 + 预置解析器

### 完成的工作

#### 1. 首页壁纸 (MovieHomeFragment.java)
- 顶部添加半透明 ImageView 显示仓库 wallpaper 字段图片
- loadAndSaveRepo 时从仓库 JSON 提取 wallpaper 并用 Glide 加载
- restoreSite 时也自动加载 wallpaper
- 默认背景色 #1a1a2e

#### 2. 分享面板 (ShareReceiverActivity.java)
- AndroidManifest.xml 注册 SEND intent-filter 接收 text/plain 分享
- 处理 URL 分享(检测 m3u8/mp4/youtu.be)
- 显示"在影视中打开"和"复制链接"按钮
- 分享 URL 保存到 share_prefs pending_share_url

#### 3. 预置解析器 (ParserSeeder.java)
- 6 个默认解析器: 默认/虾米/爱豆/M3U8/冰豆/听乐
- 首次启动时自动写入 parse_configs 表
- 已有解析器时跳过(幂等)
- 在 MainActivity 和 MovieHomeFragment 中都调用

### 构建验证
- `./gradlew :app:assembleDebug` BUILD SUCCESSFUL
