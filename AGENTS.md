# AGENTS.md

Puff 浏览器（Android WebView 壳）：标签页/书签/历史/密码、规则代理与 Shadowsocks、广告拦截、7 语种设置页。品牌 Puff，包名 `com.felix021.puff`（debug 加 `.dev` 后缀）。仓库 `felix021/puff-browser`，本地目录习惯叫 `navigateur`。

本仓库按 **AI native** 方式组织：`docs/` 分主题记录重要信息，**改动涉及某主题时同步更新对应文档**；本文件是索引入口。

## 文档索引

| 文档 | 内容 |
|---|---|
| [docs/architecture.md](docs/architecture.md) | 模块结构与核心链路（browser/data/ui、代理与 SS、隐藏功能体系） |
| [docs/build.md](docs/build.md) | 构建铁律、debug/release 包名、签名策略、分发 |
| [docs/testing.md](docs/testing.md) | 真机验收方法论（导航/输入/动画/截图/常见坑） |
| [docs/ui-conventions.md](docs/ui-conventions.md) | 统一 UI 组件与踩坑记录（改通用组件前必读） |

## 本机构建环境（私有，不在本仓库）

签名 key、SDK 路径、adb serial、测试设备隧道等机器相关信息在 **`~/.config/puff-browser/`**（本机私有 git 仓库，远端内网 gitea `puff/config`）。新环境接入：先按该目录 AGENTS.md 配好 env.sh 与 keys，再用 `PUFF_CONFIG_DIR` 指向它（缺省路径即可）。

**公开发布纪律**：本仓库是公开仓库，任何凭据、内网主机名、IP 一律不进来，需要引用时指向 `~/.config/puff-browser` 的对应条目。

## 硬规则

1. 构建铁律：`./gradlew clean assembleDebug --no-daemon`
2. 签名 key 与其它项目隔离，**永不更换/永不重新生成**（代价见 docs/build.md）
3. 设置落盘只用 `commit()`；`BrowserSettings` 加字段必须读写两处都登记（见 docs/architecture.md）
4. UI 改动按 docs/testing.md 的验收清单过一遍再报完成

## git 工作流

改动在专用 worktree（`~/worktrees/{project}`）进行，完成后 ff 到 main 推 GitHub。commit 用中文一句话说清动机，不添加 AI 署名。
