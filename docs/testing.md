# 真机验证

构建产物必须真机验收。adb serial 等环境信息在机器本地私有配置（见根 AGENTS.md），不在本仓库。

## 导航与点击

- **不要肉眼估坐标 tap**（误差可达 ±150px）。首选 `adb shell uiautomator dump`——Compose 对话框/菜单/设置页都能 dump 出 text+bounds，取 bounds 中心点击；像素法（PIL 行聚类）兜底
- 底部手势条区域（长屏 y>2350）起手的 swipe 会把应用划回桌面；列表滚动用屏幕中部
- 新装应用不一定自动上桌面（launcher 抽屉里），验证桌面名/图标时注意

## 文本输入

- 用 AdbIME 广播而非 `input text`（后者常只上屏首字符）：`am broadcast -a ADB_INPUT_TEXT --es msg '...'`；先 `settings get secure default_input_method` 确认没被输入法抢占，必要时 `ime set com.android.adbkeyboard/.AdbIME`；清空 `ADB_CLEAR_TEXT`
- shell 里的 `#` 会被设备端 shell 当注释，URL/色值注入要注意转义（外层双引号包整条命令）

## 动画与渲染验收

- **先查 `settings get global animator_duration_scale`**：为 0 时所有动画瞬完，连拍/录屏全抓不到
- 抓动画用设备端连拍（`input tap ...; for i in 1..18; do screencap ...` 一条 shell），逐帧算区域均值
- **截图验收一律以像素数据为准**（PIL 在 dump 坐标处取样、亮度分段），AI 看图工具可能返回错误内容，只当参考

## 常见坑

- Chromium 错误页按 host 缓存：同 host 二次导航不发请求，「下载/网络没反应」可能只是撞了缓存，换 host 重试
- 设置落盘验证用 `run-as <pkg> cat shared_prefs/settings.xml`（commit 同步写，读到即真）；绕 UI 配置也可直接改 settings.xml 后 force-stop
- 部分设备 `pm revoke` 被拒（SecurityException），改运行时权限走系统设置页：`am start -a android.settings.APP_NOTIFICATION_SETTINGS --es android.provider.extra.APP_PACKAGE <pkg>`
- CDP 远程调试默认关（安全纵深），`am start --ez remote_debug true` 临时开启
- 下载链路 e2e：本机起 `python3 -m http.server` + `adb reverse tcp:PORT tcp:PORT`，页面放 `<a download>` 真实触发下载监听

## 验收清单（UI 改动）

1. uiautomator dump 量化关键 bounds（如对话框「内容底→按钮顶」间隙，正常=textPadding）
2. 长短两种内容状态各看一遍（短内容是否被撑大、长内容是否可滚到底）
3. 像素取样确认视觉特征（滚动条/高亮/颜色联动）
