# 架构

单 Activity（`MainActivity`）+ Jetpack Compose，无多模块拆分。命名空间 `com.felix021.puff`。

## 顶层

- `PuffApp` — Application，持 `AppContainer`（手工 DI：settings/广告/历史等仓库单例）
- `MainActivity` — 唯一 Activity；`BrowserController` 门面在此构造；处理 remote_debug intent、通知权限结果转发
- `ui/BrowserController` — UI 层与 browser/data 层之间唯一门面，Activity 重建时复用

## browser/（WebView 引擎）

- `TabManager` — WebView 池（每标签一个 WebView）、标签会话恢复（`TabSessionStore`）、下载监听、JS 桥注册。Activity 销毁时无条件 `destroyAll()`（语言切换会重建 Activity）
- `WebViewFactory` — WebView 统一配置：UA 预设解析、页面缩放（CSS zoom）、暗色强制、桌面模式
- `BrowserWebViewClient` — 广告拦截（`shouldInterceptRequest`）、下载触发（→ `util/Downloader`）
- `BrowserChromeClient` — 页面 alert/confirm 等系统对话框
- `NativeBridge` — 注入页面的 `PuffBridge` JS 桥（下载、凭据保存）
- `JsScripts.kt` — 注入脚本资产（eruda 开发者工具、字体 CSS）
- `AdBlockEngine` — EasyList 规则匹配（内置规则资产 + 在线更新）

## data/

- `SettingsRepository` — 唯一设置源：`StateFlow<BrowserSettings>`，SharedPreferences 写盘用 **commit 而非 apply**（部分 ROM 上 apply 丢数据）。所有设置项在 `BrowserSettings` data class 上加字段 + 读写两处登记
- `ProxyRepository` — 规则代理（JSON 持久化在 settings.proxyJson）：模式（直连/固定出口/按规则）、出口列表、规则列表、AutoProxy/gfwlist 导入
- `SsCrypto` / `SsTunnel` — Shadowsocks 支持：AEAD 加解密 + 本地 SOCKS5 隧道（出口类型 SS 走它，系统下载等场景与规则代理共用）
- `PasswordStore` — 已保存密码，Android Keystore AES-GCM 加密（`security/CryptoBox`）
- `BookmarkRepository` / `HistoryRepository` / `TabSessionStore` / `UaPresets` / `SearchEngines`

## ui/

- `AppRoot` → `BrowserScreen`（地址栏/底栏/菜单/起始页）+ 设置二级页栈（`SettingsScreen` 内 `SubPage` 枚举：常规/显示/网站/更多/关于等）
- `ui/screen/Settings*` — 每主题一个文件；通用行组件在 `SettingsCommon.kt`（`SettingItem`/`SwitchItem`/呼吸灯高亮）
- `ui/component/` — 跨场景复用组件，**新 UI 先看这里有没有现成的**：
  - `AppDialog` 全应用统一对话框壳（text 槽自动滚动+滚动条）
  - `StepSlider` 带两端 +/- 步进键的离散滑块
  - `TriangleThumb` 双三角滑块 thumb（上倒三角/下正三角夹轨道）
- `Theme.kt` — 主题色系统：预设 ACCENTS + 自定义色相（`accentColorScheme`）

## 隐藏功能

- 代理出口的 SS 类型默认隐藏；关于页版本号 300ms 内连点 6 次解锁（`unlockSs` 持久化）。以后隐藏功能统一挂这个开关体系

## util/

- `Downloader` — 系统DownloadManager 封装；入口处有通知权限 gate（`NotifPermission`：首次下载先解释再请求系统授权，拒绝则本会话不再问、下载照常）
