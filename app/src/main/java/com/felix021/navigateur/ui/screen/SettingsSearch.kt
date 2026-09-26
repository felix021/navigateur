package com.felix021.navigateur.ui.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.felix021.navigateur.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 设置项搜索注册表：一个设置项一条。
 * title 走资源（随界面语言）；desc/keywords 是中英混合的意图词，仅用于匹配、不随界面翻译
 * （假设：搜索输入以中/英为主）。
 */
internal data class SearchEntry(
    val id: String,
    @StringRes val titleRes: Int,
    /** 搜索用描述（不出现在设置界面上） */
    val desc: String,
    /** 意图同义词：用户可能输入的说法 */
    val keywords: List<String>,
    /** 所属页面；null = 设置一级页 */
    val page: SubPage?,
)

internal val SETTINGS_ENTRIES: List<SearchEntry> = listOf(
    // 一级页 · 常规
    SearchEntry("s.homepage", R.string.settings_homepage, "设置新标签页默认打开的网址", listOf("首页", "home", "主页地址", "起始页", "start page", "homepage", "默认打开"), null),
    SearchEntry("s.search_engine", R.string.settings_search_engine, "选择默认搜索引擎", listOf("搜索", "search", "默认引擎", "百度", "google", "bing", "duckduckgo", "搜索引擎切换"), null),
    SearchEntry("s.language", R.string.settings_language, "界面显示语言，跟随系统或手动指定", listOf("语言", "language", "多国语言", "i18n", "english", "中文", "法语", "日语", "翻译", "外语"), null),
    SearchEntry("s.save_passwords", R.string.settings_save_passwords, "登录时询问是否保存账号密码", listOf("密码", "password", "保存密码", "自动填充", "credential", "login", "账号"), null),
    // 一级页 · 显示
    SearchEntry("s.theme", R.string.settings_theme, "亮色 / 暗色 / 跟随系统", listOf("主题", "theme", "暗色", "深色", "夜间", "dark", "light", "night", "亮色", "黑色模式"), null),
    SearchEntry("s.zoom", R.string.settings_zoom, "整体缩放网页内容（含布局与图片）", listOf("缩放", "zoom", "放大", "字号", "字体大小", "scale", "看得清", "字太小"), null),
    SearchEntry("s.font", R.string.settings_font, "网页字体样式：无衬线 / 衬线 / 等宽", listOf("字体", "font", "衬线", "等宽", "serif", "monospace", "字体样式"), null),
    SearchEntry("s.land_side", R.string.settings_landscape_side, "横屏时工具条放左边还是右边", listOf("横屏", "landscape", "工具栏", "toolbar", "左手", "左右", "单手"), null),
    SearchEntry("s.land_fullscreen", R.string.settings_landscape_fullscreen, "横屏隐藏系统状态栏", listOf("横屏", "全屏", "fullscreen", "状态栏", "status bar", "沉浸", "视野"), null),
    SearchEntry("s.land_safe", R.string.settings_landscape_safe_area, "横屏是否显示并避让手势条", listOf("安全区", "手势条", "gesture", "导航条", "navigation bar", "底部", "黑边"), null),
    // 一级页 · 网站
    SearchEntry("s.desktop_default", R.string.settings_desktop_default, "新标签默认用桌面版 UA 打开", listOf("桌面", "desktop", "电脑版", "PC", "默认UA", "响应式"), null),
    SearchEntry("s.ua", R.string.settings_ua, "浏览器标识字符串，可伪装机型或用自定义预设", listOf("ua", "user-agent", "伪装", "浏览器标识", "agent", "机型", "iPhone", "自定义UA", "预设"), null),
    // 一级页 · 更多（入口）
    SearchEntry("s.adblock", R.string.entry_adblock, "广告拦截总开关、规则更新与白名单", listOf("广告", "adblock", "弹窗", "popup", "tracking", "跟踪", "拦截", "去广告", "easylist"), null),
    SearchEntry("s.proxy", R.string.entry_proxy, "代理出口、分流规则与规则导入", listOf("代理", "proxy", "vpn", "翻墙", "gfw", "科学上网", "节点", "分流", "规则"), null),
    SearchEntry("s.dev", R.string.entry_developer, "远程调试与页面内 eruda 工具", listOf("开发者", "developer", "调试", "debug", "eruda", "console", "inspect", "cdp", "远程"), null),
    SearchEntry("s.privacy", R.string.entry_privacy, "清理浏览数据与按站点清理", listOf("隐私", "privacy", "清理", "缓存", "clear", "cookies", "历史", "history", "数据"), null),
    // 广告拦截页
    SearchEntry("a.switch", R.string.adblock_switch, "广告拦截总开关", listOf("开关", "总开关", "enable", "关闭广告", "启用", "停用"), SubPage.AdBlock),
    SearchEntry("a.update", R.string.adblock_update, "在线更新 EasyList 规则库", listOf("更新", "update", "规则库", "easylist", "刷新", "升级"), SubPage.AdBlock),
    SearchEntry("a.allowlist", R.string.adblock_allowlist, "对某些站点不拦截广告", listOf("白名单", "whitelist", "不拦截", "例外", "exclude", "allowlist", "放行"), SubPage.AdBlock),
    // 代理页
    SearchEntry("p.mode", R.string.proxy_current_mode, "直连 / 固定出口 / 自动切换", listOf("模式", "mode", "直连", "自动切换", "固定", "切换"), SubPage.Proxy),
    SearchEntry("p.outlets", R.string.proxy_outlets, "配置 HTTP / SOCKS5 代理服务器", listOf("出口", "outlet", "节点", "服务器", "server", "socks", "host", "port", "端口", "添加节点"), SubPage.Proxy),
    SearchEntry("p.rules", R.string.proxy_rules_title, "按域名设置走代理还是直连", listOf("规则", "rule", "分流", "域名", "domain", "直连规则", "代理规则", "pattern"), SubPage.Proxy),
    SearchEntry("p.import", R.string.proxy_import_entry, "粘贴或从 URL 下载 AutoProxy / gfwlist 列表", listOf("导入", "import", "粘贴", "paste", "autoproxy", "gfwlist", "订阅", "下载规则", "规则列表"), SubPage.Proxy),
    // 开发者页
    SearchEntry("d.remote", R.string.dev_remote_title, "电脑经 adb 用 Chrome DevTools 调试", listOf("远程调试", "remote", "chrome://inspect", "cdp", "adb", "devtools", "电脑调试"), SubPage.Developer),
    SearchEntry("d.eruda", R.string.dev_eruda_title, "手机页面内 Console / Elements / Network", listOf("eruda", "console", "elements", "network", "storage", "页面内", "调试工具", "查看源码"), SubPage.Developer),
    // 关于页
    SearchEntry("s.about", R.string.entry_about, "版本号、GitHub 仓库、隐私说明与开源组件", listOf("关于", "about", "版本", "version", "开源", "协议", "license", "github", "作者", "致谢"), null),
    SearchEntry("about.github", R.string.about_github, "项目源码仓库地址", listOf("github", "仓库", "源码", "代码", "repository", "开源地址"), SubPage.About),
    SearchEntry("about.privacy", R.string.about_privacy_title, "数据存哪里、会不会上传", listOf("隐私政策", "privacy policy", "数据收集", "不上传", "泄露", "tracking", "数据"), SubPage.About),
    SearchEntry("about.licenses", R.string.about_licenses_title, "引用的开源库与许可证", listOf("开源许可", "license", "third party", "依赖", "致谢", "oss", "协议", "gpl"), SubPage.About),
    // 隐私页
    SearchEntry("pv.clear_all", R.string.privacy_clear_all, "一次性清除 Cookie / 存储 / 缓存 / 历史 / 密码", listOf("清理", "缓存", "cache", "cookie", "历史", "密码", "表单", "form", "全部清除", "clear data"), SubPage.Privacy),
    SearchEntry("pv.clear_site", R.string.privacy_clear_site, "只清除某个站点的数据", listOf("按站点", "单个网站", "site", "域名清理", "host", "指定站点"), SubPage.Privacy),
)

/** 按查询串匹配设置项：标题（本地化）> 关键词 > 描述；同分保持注册表顺序 */
internal fun matchSettings(query: String, context: android.content.Context): List<SearchEntry> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return emptyList()
    return SETTINGS_ENTRIES.mapNotNull { e ->
        val title = context.getString(e.titleRes).lowercase()
        val kw = e.keywords.map { it.lowercase() }
        val score = when {
            title.startsWith(q) -> 0
            title.contains(q) -> 1
            kw.any { it.startsWith(q) } -> 2
            kw.any { it.contains(q) } -> 3
            e.desc.lowercase().contains(q) -> 4
            else -> return@mapNotNull null
        }
        score to e
    }.sortedBy { it.first }.map { it.second }
}

/** 一次跳转请求：nonce 保证重复点同一项也能触发；page 标明该由哪个页面消费 */
internal data class JumpRequest(val id: String, val nonce: Int, val page: SubPage?)

/**
 * 设置页共享导航状态：定位坐标（供滚动）、跳转目标、呼吸灯高亮。
 * 由 SettingsScreen 创建并经 [LocalSettingsNav] 下发。
 */
internal class SettingsNav {
    /** 设置项 id → 在 root 坐标系中的 y（滚动时由 onGloballyPositioned 持续更新） */
    val positions: SnapshotStateMap<String, Int> = mutableStateMapOf()
    /** 当前滚动列表容器顶的 y（root 坐标） */
    var listTop by mutableStateOf(0)
    var jump by mutableStateOf<JumpRequest?>(null)
    var highlightId by mutableStateOf<String?>(null)
    private var nonce = 0
    private var consumedNonce = 0

    fun requestJump(entry: SearchEntry, setSub: (SubPage?) -> Unit) {
        setSub(entry.page)
        jump = JumpRequest(entry.id, ++nonce, entry.page)
    }

    /**
     * 取属于 [page] 且未被消费的跳转。消费式协议解决两个问题：
     * 页面重组时 effect 重放旧跳转；跨页切换时源页面 effect 抢先消费新跳转。
     */
    fun consumeJump(page: SubPage?): JumpRequest? {
        val j = jump ?: return null
        if (j.page != page || j.nonce == consumedNonce) return null
        consumedNonce = j.nonce
        return j
    }

    fun clearHighlight() {
        highlightId = null
    }
}

/** 一级页/二级页的列表 Column 挂这个：上报容器顶坐标 */
@Composable
internal fun Modifier.settingsList(): Modifier {
    val nav = LocalSettingsNav.current
    return onGloballyPositioned { coords ->
        nav?.listTop = coords.positionInRoot().y.toInt()
    }
}

/**
 * 页面级跳转执行器：挂在每个滚动页面里，[page] 为本页标识（一级页传 null）。
 * 流程：等目标项上报坐标 → 滚到其下方留白处 → 触发呼吸灯高亮。
 */
@Composable
internal fun SettingsJumpTarget(scroll: androidx.compose.foundation.ScrollState, page: SubPage?) {
    val nav = LocalSettingsNav.current ?: return
    val density = LocalDensity.current
    LaunchedEffect(nav.jump) {
        val req = nav.consumeJump(page) ?: return@LaunchedEffect
        // 等目标项完成布局并上报坐标（切页后新页面首帧）
        val pos = withTimeoutOrNull(1200) {
            snapshotFlow { nav.positions[req.id] }.first { it != null }
        } ?: return@LaunchedEffect
        val offsetPx = with(density) { 88.dp.toPx() }
        val want = (pos - nav.listTop + scroll.value - offsetPx).toInt().coerceAtLeast(0)
        scroll.animateScrollTo(want)
        nav.highlightId = req.id
    }
}

/** 设置页顶部搜索框：输入即出下拉候选（与搜索框同宽的自绘样式菜单），点击跳转 */
@Composable
internal fun SettingsSearchBar(onSelect: (SearchEntry) -> Unit) {
    val context = LocalContext.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    var query by remember { mutableStateOf("") }
    val matches = remember(query) { matchSettings(query, context) }
    val expanded = query.isNotBlank() && matches.isNotEmpty()

    androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text(stringResource(R.string.settings_search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_clear_input))
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { query = "" },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 3.dp,
            shadowElevation = 6.dp,
        ) {
            matches.forEach { e ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(stringResource(e.titleRes), style = MaterialTheme.typography.bodyLarge)
                            e.page?.let {
                                Text(
                                    stringResource(it.titleRes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    onClick = {
                        query = ""
                        // 收起键盘：跳转目标定位在列表上部，别让 IME 挡住
                        focusManager.clearFocus()
                        onSelect(e)
                    },
                )
            }
        }
    }
}
