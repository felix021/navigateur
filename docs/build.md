# 构建与发布

## 铁律

**`./gradlew clean assembleDebug --no-daemon`** —— 本机（PVE 容器）增量构建容易出玄学失败，一律 clean + no-daemon，别省这几分钟。

## 构建类型（2026-09-27 起）

| | 包名 | 桌面名 | 说明 |
|---|---|---|---|
| debug | `com.felix021.puff.dev` | Puff Nav dev | `applicationIdSuffix` + debug sourceSet 覆盖 `app_name`（7 语种） |
| release | `com.felix021.puff` | Puff Nav | 无混淆（个人应用，minify 关闭） |

- 两者**同签名**（puff 专属 key），可并存互不覆盖
- debug 包 `am start` 必须用全类名：`com.felix021.puff.dev/com.felix021.puff.MainActivity`（`.MainActivity` 简写会按 applicationId 展开而报错）

## 签名

- puff 专属独立 key（**不与其他项目共享**），凭据在机器本地私有仓库（见根 AGENTS.md 的「本机构建环境」），主仓库只含路径逻辑、无凭据
- 可用环境变量 `PUFF_CONFIG_DIR` 重定向配置目录，缺省 `~/.config/puff-browser`
- **不要换 key**：签名变了，已装设备「升级」会变成卸载重装、清空用户数据

## 分发

GitHub 仓库 `felix021/puff-browser`（公开），release APK 直接分发。发布前自查：无硬编码凭据/私有主机名，字符串资源完整。
