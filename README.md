# Navigateur

自用 Android 浏览器（Kotlin + Jetpack Compose + WebView）。

## 功能

- **多标签**：每标签独立 WebView，卡片式切换器，重启后恢复
- **起始页**：内置起始页（搜索框 + 收藏宫格），主页可在设置中改为任意 URL
- **书签**：底栏星标收藏，起始页/书签页增删改
- **桌面/移动模式**：菜单快捷切换（改 UA 后刷新），设置里可设默认 + 自定义 UA
- **字号/字体**：textZoom 50–200% 即时生效；字体（无衬线/衬线/等宽）CSS 注入
- **亮/暗主题**：跟随系统/亮色/暗色，网页内容走 algorithmic darkening
- **密码记录**：登录表单捕获 → 询问保存（Android Keystore AES-256-GCM 加密落盘）→ 自动填充
- **清理数据**：Cookie / 站点存储（localStorage 等）/ 缓存 / 表单数据 / 已保存密码
- **搜索引擎**：DDG / Google / Bing / 百度

## 构建

```bash
./gradlew clean assembleDebug --no-daemon
# 产物: app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

> 本机（PVE）注意事项：Gradle daemon 的增量构建不可靠（源码变更可能被误判
> UP-TO-DATE），务必 `clean` + `--no-daemon` 构建。

## 已知限制（v1）

- fetch/XHR 方式提交的登录、跨域 iframe 表单、passkey 无法捕获
- `target=_blank` 链接在当前标签打开
- 无下载管理、无图片/资源拦截
- 桌面模式仅切换 UA + 视口，个别站点仍按屏幕宽度出移动版
