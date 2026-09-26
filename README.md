# Navigateur

自用 Android 浏览器（Kotlin + Jetpack Compose + WebView）。

## 功能

- **多标签**：每标签独立 WebView，卡片式切换器，重启后恢复
- **起始页**：内置起始页（搜索框 + 收藏宫格），主页可在设置中改为任意 URL
- **书签**：底栏星标收藏，起始页/书签页增删改
- **桌面/移动模式**：菜单快捷切换（改 UA 后刷新），设置里可设默认 + 管理多个自定义 UA 预设
- **字号/字体**：textZoom 50–200% 即时生效；字体（无衬线/衬线/等宽）CSS 注入
- **亮/暗主题**：跟随系统/亮色/暗色，网页内容走 algorithmic darkening
- **多语言**：默认跟随系统，可切换 中文 / English / Français / 日本語 / Русский / Deutsch / Español
- **设置搜索**：设置页顶部搜索框，按名称或意图关键词（同义词表，离线）定位设置项，
  点击候选跳转滚动并呼吸灯高亮提示
- **密码记录**：登录表单捕获（含无 form 的 fetch/XHR 登录、同域 iframe）→ 询问保存（Android Keystore AES-256-GCM 加密落盘）→ 自动填充
- **清理数据**：Cookie / 站点存储（localStorage 等）/ 缓存 / 表单数据 / 已保存密码
- **下载**：系统 DownloadManager 接管，落公共下载目录、通知栏进度；页面内常见下载链接与地址栏直输文件 URL 均可
- **广告拦截**：EasyList + EasyList China（域名拦截 + 元素隐藏 CSS 注入）；
  设置页总开关 / 更新规则 / 站点白名单，浏览器菜单可对当前站点单独关闭
- **规则代理**（类 SwitchyOmega / AutoProxy）：多出口（HTTP/SOCKS5）+
  自动切换，未命中默认可选「走代理」或「直连」（后者配 AutoProxy/gfwlist
  语义，按当前站点命中动态开关）；规则支持逐条添加或整体导入
  （粘贴 / URL 下载，AutoProxy、gfwlist 格式，gfwlist base64 自动解码）；
  浏览器菜单快切；基于 WebView ProxyController，切换即时生效
- **开发者工具**：设置开启页面内 eruda（Console/Elements/Network/Storage，
  手机直接用）；远程调试（chrome://inspect 完整 DevTools）默认关闭，
  可 `am start --ez remote_debug true` 快捷开启
- **搜索引擎**：DDG / Google / Bing / 百度
- **关于页**：版本号、GitHub 入口、隐私说明与开源组件许可证声明

## 构建

```bash
./gradlew clean assembleDebug   # 调试包
./gradlew clean assembleRelease # 发布包（debug 签名，个人分发用）
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

> 提示：如遇增量构建产物不更新（部分虚拟机环境存在该问题），加 `clean` 全量构建。

## 已知限制

- 跨域 iframe 表单、passkey 登录无法捕获
- 下载：无扩展名且无 `download` 属性的重定向下载链接（个别网盘）可能不触发；
  新版 WebView（15x）已不回调 `onDownloadStart`，靠页面层拦截兜底
- 广告拦截为 EasyList 主流子集（整域拦/例外/域名级元素隐藏），路径级与通用元素规则未解析；
  无扩展名的纯重定向下载类广告资源可能漏拦
- 代理基于 ProxyController：上游无 PAC/多出口分流，不支持按域名走不同
  代理；「未命中直连」模式按**主文档**站点命中动态开关代理，同页面的
  子资源跟随主文档走向（多标签同时打开不同走向的站点时以后打开的为准）
