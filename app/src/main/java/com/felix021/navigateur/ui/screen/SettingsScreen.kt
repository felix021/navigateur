package com.felix021.navigateur.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.felix021.navigateur.data.SearchEngines
import com.felix021.navigateur.data.ThemeMode
import com.felix021.navigateur.data.UaPresets
import com.felix021.navigateur.ui.BrowserController
import com.felix021.navigateur.ui.Screen
import com.felix021.navigateur.ui.component.SingleChoiceDialog
import com.felix021.navigateur.ui.component.TextInputDialog
import com.felix021.navigateur.util.UrlUtils

private val THEME_LABELS = mapOf(
    ThemeMode.FOLLOW_SYSTEM to "跟随系统",
    ThemeMode.LIGHT to "亮色",
    ThemeMode.DARK to "暗色",
)

private val FONT_OPTIONS = listOf(
    "" to "默认（跟随网站）",
    "sans-serif" to "无衬线",
    "serif" to "衬线",
    "monospace" to "等宽",
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(controller: BrowserController) {
    val settings by controller.container.settings.settings.collectAsState()
    var showHome by remember { mutableStateOf(false) }
    var showEngine by remember { mutableStateOf(false) }
    var showTheme by remember { mutableStateOf(false) }
    var showFont by remember { mutableStateOf(false) }
    var showUa by remember { mutableStateOf(false) }
    var showUaCustom by remember { mutableStateOf(false) }
    var showClear by remember { mutableStateOf(false) }
    var showSiteClear by remember { mutableStateOf(false) }
    var showLandSide by remember { mutableStateOf(false) }
    var showAdAllow by remember { mutableStateOf(false) }
    var showProxyMode by remember { mutableStateOf(false) }
    var showProxyProfiles by remember { mutableStateOf(false) }
    var showProxyRules by remember { mutableStateOf(false) }
    val adStatus by controller.container.adBlock.status.collectAsState()

    val update: ((com.felix021.navigateur.data.BrowserSettings) -> com.felix021.navigateur.data.BrowserSettings) -> Unit =
        { controller.container.settings.update(it) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = { controller.screen.value = Screen.Browser }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader("常规")
            SettingItem(
                title = "主页",
                value = if (UrlUtils.isHome(settings.homepage)) "内置起始页" else settings.homepage,
            ) { showHome = true }
            SettingItem(
                title = "搜索引擎",
                value = SearchEngines.byId(settings.searchEngineId).name,
            ) { showEngine = true }
            SwitchItem(
                title = "记录密码",
                checked = settings.savePasswords,
                onChange = { enabled -> update { it.copy(savePasswords = enabled) } },
            )

            SectionHeader("显示")
            SettingItem(
                title = "主题",
                value = THEME_LABELS[settings.themeMode].orEmpty(),
            ) { showTheme = true }
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("页面缩放", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${settings.pageZoomPercent}%",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Slider(
                    value = settings.pageZoomPercent.toFloat(),
                    onValueChange = { update { s -> s.copy(pageZoomPercent = it.toInt().coerceIn(50, 200)) } },
                    valueRange = 50f..200f,
                )
            }
            SettingItem(
                title = "字体",
                value = FONT_OPTIONS.firstOrNull { it.first == settings.fontFamily }?.second.orEmpty(),
            ) { showFont = true }
            SettingItem(
                title = "横屏工具栏位置",
                value = if (settings.landscapeToolbarSide == "left") "左手边" else "右手边",
            ) { showLandSide = true }
            SwitchItem(
                title = "横屏全屏",
                subtitle = "横屏时隐藏系统状态栏，充分利用屏幕高度",
                checked = settings.landscapeFullscreen,
                onChange = { enabled -> update { it.copy(landscapeFullscreen = enabled) } },
            )
            SwitchItem(
                title = "横屏底部安全区",
                subtitle = "显示系统手势条并避让；关闭则完全全屏（自绘状态信息仍在）",
                checked = settings.landscapeBottomSafeArea,
                onChange = { enabled -> update { it.copy(landscapeBottomSafeArea = enabled) } },
            )

            SectionHeader("网站")
            SwitchItem(
                title = "默认桌面模式",
                subtitle = "新标签页默认以桌面 UA 打开（UA 预设为默认时生效）",
                checked = settings.desktopModeDefault,
                onChange = { enabled -> update { it.copy(desktopModeDefault = enabled) } },
            )
            SettingItem(
                title = "User-Agent",
                value = uaDisplay(settings),
            ) { showUa = true }

            SectionHeader("广告拦截")
            SwitchItem(
                title = "拦截广告",
                subtitle = "基于 EasyList + EasyList China，拦截请求并隐藏页面广告元素",
                checked = settings.adBlockEnabled,
                onChange = { enabled -> update { it.copy(adBlockEnabled = enabled) } },
            )
            SettingItem(
                title = "更新广告规则",
                value = adRulesLabel(adStatus),
            ) { controller.container.adBlock.update() }
            SettingItem(
                title = "站点白名单",
                value = if (settings.adBlockAllowlist.isEmpty()) "无（全部站点生效）"
                else "${settings.adBlockAllowlist.size} 个站点不拦截",
            ) { showAdAllow = true }

            SectionHeader("代理")
            val proxy = com.felix021.navigateur.data.ProxyRepository.fromJson(settings.proxyJson)
            SettingItem(
                title = "当前模式",
                value = proxyModeLabel(proxy),
            ) { showProxyMode = true }
            SettingItem(
                title = "代理出口",
                value = if (proxy.profiles.isEmpty()) "未添加" else proxy.profiles.joinToString(" / ") { it.name },
            ) { showProxyProfiles = true }
            SettingItem(
                title = "自动切换规则",
                value = if (proxy.rules.isEmpty()) "未添加" else "${proxy.rules.size} 条（命中直连）",
            ) { showProxyRules = true }

            SectionHeader("开发者")
            SwitchItem(
                title = "远程调试（chrome://inspect / CDP）",
                subtitle = "默认关闭。开启后电脑经 adb 可用完整 Chrome DevTools 检查页面；" +
                    "也可用命令行开关：am start --ez remote_debug true",
                checked = settings.remoteDebug,
                onChange = { enabled -> update { it.copy(remoteDebug = enabled) } },
            )
            SwitchItem(
                title = "页面内开发者工具（eruda）",
                subtitle = "手机上直接查看 Console / Elements / Network / Storage",
                checked = settings.devTools,
                onChange = { enabled -> update { it.copy(devTools = enabled) } },
            )

            SectionHeader("隐私")
            SettingItem(
                title = "清理浏览数据",
                value = "Cookie / 站点存储 / 缓存 / 历史 / 密码（全部）",
            ) { showClear = true }
            SettingItem(
                title = "按站点清理",
                value = "清除指定站点的存储 / Cookie / 密码",
            ) { showSiteClear = true }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showHome) {
        TextInputDialog(
            title = "主页",
            initial = settings.homepage,
            label = "网址",
            supportingText = "填 about:home 使用内置起始页；否则新标签页直接加载该网址",
            onDismiss = { showHome = false },
            onOk = { v ->
                update { it.copy(homepage = v.ifBlank { UrlUtils.HOME }) }
                showHome = false
            },
        )
    }
    if (showEngine) {
        SingleChoiceDialog(
            title = "搜索引擎",
            options = SearchEngines.ALL,
            selected = SearchEngines.byId(settings.searchEngineId),
            label = { it.name },
            onDismiss = { showEngine = false },
            onSelect = {
                update { s -> s.copy(searchEngineId = it.id) }
                showEngine = false
            },
        )
    }
    if (showTheme) {
        SingleChoiceDialog(
            title = "主题",
            options = ThemeMode.entries.toList(),
            selected = settings.themeMode,
            label = { THEME_LABELS[it].orEmpty() },
            onDismiss = { showTheme = false },
            onSelect = {
                update { s -> s.copy(themeMode = it) }
                showTheme = false
            },
        )
    }
    if (showFont) {
        SingleChoiceDialog(
            title = "字体",
            options = FONT_OPTIONS,
            selected = FONT_OPTIONS.firstOrNull { it.first == settings.fontFamily },
            label = { it.second },
            onDismiss = { showFont = false },
            onSelect = {
                update { s -> s.copy(fontFamily = it.first) }
                showFont = false
            },
        )
    }
    if (showLandSide) {
        SingleChoiceDialog(
            title = "横屏工具栏位置",
            options = listOf("left", "right"),
            selected = settings.landscapeToolbarSide,
            label = { if (it == "left") "左手边" else "右手边" },
            onDismiss = { showLandSide = false },
            onSelect = {
                update { s -> s.copy(landscapeToolbarSide = it) }
                showLandSide = false
            },
        )
    }
    if (showProxyMode) {
        ProxyModeDialog(
            proxy = com.felix021.navigateur.data.ProxyRepository.fromJson(settings.proxyJson),
            onSelect = { mode, autoId ->
                update { s ->
                    val p = com.felix021.navigateur.data.ProxyRepository.fromJson(s.proxyJson)
                    s.copy(proxyJson = com.felix021.navigateur.data.ProxyRepository.toJson(p.copy(mode = mode, autoProfileId = autoId)))
                }
                showProxyMode = false
            },
            onDismiss = { showProxyMode = false },
        )
    }
    if (showProxyProfiles) {
        ProxyProfilesDialog(
            proxy = com.felix021.navigateur.data.ProxyRepository.fromJson(settings.proxyJson),
            onSave = { newList ->
                update { s ->
                    val p = com.felix021.navigateur.data.ProxyRepository.fromJson(s.proxyJson)
                    s.copy(proxyJson = com.felix021.navigateur.data.ProxyRepository.toJson(p.copy(profiles = newList)))
                }
            },
            onDismiss = { showProxyProfiles = false },
        )
    }
    if (showProxyRules) {
        ProxyRulesDialog(
            proxy = com.felix021.navigateur.data.ProxyRepository.fromJson(settings.proxyJson),
            onSave = { newRules ->
                update { s ->
                    val p = com.felix021.navigateur.data.ProxyRepository.fromJson(s.proxyJson)
                    s.copy(proxyJson = com.felix021.navigateur.data.ProxyRepository.toJson(p.copy(rules = newRules)))
                }
            },
            onDismiss = { showProxyRules = false },
        )
    }
    if (showAdAllow) {
        AdAllowlistDialog(
            hosts = settings.adBlockAllowlist,
            onRemove = { host ->
                update { s -> s.copy(adBlockAllowlist = s.adBlockAllowlist - host) }
            },
            onDismiss = { showAdAllow = false },
        )
    }
    if (showUa) {
        SingleChoiceDialog(
            title = "User-Agent",
            options = UaPresets.ALL,
            selected = UaPresets.byId(settings.uaPresetId),
            label = { it.label },
            onDismiss = { showUa = false },
            onSelect = {
                showUa = false
                if (it.id == UaPresets.CUSTOM) {
                    showUaCustom = true
                } else {
                    update { s -> s.copy(uaPresetId = it.id) }
                }
            },
        )
    }
    if (showUaCustom) {
        TextInputDialog(
            title = "自定义 User-Agent",
            initial = settings.customUserAgent,
            label = "UA 字符串",
            supportingText = "保存后立即生效并刷新已打开页面；留空则回退默认",
            onDismiss = { showUaCustom = false },
            onOk = { v ->
                update { it.copy(uaPresetId = UaPresets.CUSTOM, customUserAgent = v) }
                showUaCustom = false
            },
        )
    }
    if (showSiteClear) {
        SiteClearDialog(
            hosts = controller.knownHosts(),
            onDismiss = { showSiteClear = false },
            onClear = { host, cookies, storage, pw, his ->
                controller.clearSiteData(host, cookies, storage, pw, his)
                showSiteClear = false
            },
        )
    }
    if (showClear) {
        ClearDataDialog(
            onDismiss = { showClear = false },
            onClear = { cookies, storage, cache, form, pw, his ->
                controller.clearData(cookies, storage, cache, form, pw, his)
                showClear = false
            },
        )
    }
}

private fun uaDisplay(s: com.felix021.navigateur.data.BrowserSettings): String {
    val preset = UaPresets.byId(s.uaPresetId)
    return if (preset.id == UaPresets.CUSTOM) {
        s.customUserAgent.ifBlank { "默认（本机）" }
    } else {
        preset.label
    }
}

/** 按站点清理：输入时下拉匹配已知站点（密码/书签/历史），勾选要清理的类别 */
@Composable
private fun SiteClearDialog(
    hosts: List<String>,
    onDismiss: () -> Unit,
    onClear: (host: String, cookies: Boolean, storage: Boolean, passwords: Boolean, history: Boolean) -> Unit,
) {
    var host by remember { mutableStateOf("") }
    var clearCookies by remember { mutableStateOf(true) }
    var clearStorage by remember { mutableStateOf(true) }
    var clearPasswords by remember { mutableStateOf(true) }
    var clearHistory by remember { mutableStateOf(true) }
    // 输入为空显示全部（最多 8 个），否则按前缀/包含过滤
    val matched = remember(host, hosts) {
        if (host.isBlank()) hosts.take(8)
        else hosts.filter { it.contains(host, ignoreCase = true) }.take(8)
    }.filter { it != host }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("按站点清理") },
        text = {
            Column(
                Modifier
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                androidx.compose.material3.OutlinedTextField(
                    value = host,
                    onValueChange = { host = it.trim() },
                    label = { Text("站点域名") },
                    singleLine = true,
                )
                if (matched.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    matched.take(5).forEach { h ->
                        Text(
                            h,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { host = h }
                                .padding(vertical = 7.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Spacer(Modifier.height(4.dp))
                CheckRow("Cookie", clearCookies) { clearCookies = it }
                CheckRow("站点存储（localStorage 等）", clearStorage) { clearStorage = it }
                CheckRow("已保存密码", clearPasswords) { clearPasswords = it }
                CheckRow("浏览历史（该站点）", clearHistory) { clearHistory = it }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (host.isNotBlank()) onClear(host, clearCookies, clearStorage, clearPasswords, clearHistory)
                },
                enabled = host.isNotBlank(),
            ) { Text("清理") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun adRulesLabel(s: com.felix021.navigateur.data.AdBlockStatus): String = when {
    s.updating -> "正在更新…"
    s.lastError != null -> "更新失败：${s.lastError}（点击重试）"
    s.updatedAt > 0 -> {
        val d = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.CHINA).format(java.util.Date(s.updatedAt))
        "${s.ruleCount} 条规则 · $d 更新"
    }
    else -> "${s.ruleCount} 条规则 · 内置版本（点击在线更新）"
}

/** 广告拦截站点白名单管理 */
@Composable
private fun AdAllowlistDialog(
    hosts: Set<String>,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("站点白名单") },
        text = {
            Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                if (hosts.isEmpty()) {
                    Text(
                        "白名单为空，广告拦截在所有站点生效。\n浏览器菜单里可对当前站点单独关闭。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                hosts.sorted().forEach { h ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(h, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        IconButton(onClick = { onRemove(h) }) {
                            Icon(Icons.Filled.Close, contentDescription = "移除", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } },
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        Modifier.fillMaxWidth().padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleSmall,
    )
}

@Composable
private fun SettingItem(title: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SwitchItem(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ClearDataDialog(
    onDismiss: () -> Unit,
    onClear: (cookies: Boolean, storage: Boolean, cache: Boolean, formData: Boolean, passwords: Boolean, history: Boolean) -> Unit,
) {
    val labels = listOf(
        "Cookie",
        "站点存储（localStorage 等）",
        "缓存",
        "表单数据",
        "已保存密码",
        "浏览历史",
    )
    val checked = remember { mutableStateListOf(true, true, true, true, false, true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("清理浏览数据") },
        text = {
            Column {
                labels.forEachIndexed { i, label ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable { checked[i] = !checked[i] }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = checked[i], onCheckedChange = { checked[i] = it })
                        Spacer(Modifier.padding(4.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onClear(checked[0], checked[1], checked[2], checked[3], checked[4], checked[5])
            }) { Text("清理") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

// ---------- 代理 ----------

private fun proxyModeLabel(p: com.felix021.navigateur.data.ProxySettings): String = when (p.mode) {
    "direct" -> "直连"
    "auto" -> {
        val name = p.profiles.firstOrNull { it.id == p.autoProfileId }?.name ?: "（未设出口）"
        "自动切换（出口：$name）"
    }
    else -> p.profiles.firstOrNull { it.id == p.mode }?.let { "固定：${it.name}" } ?: "直连"
}

/** 模式选择：直连 / 各出口 / 自动切换（出口在下拉里选） */
@Composable
private fun ProxyModeDialog(
    proxy: com.felix021.navigateur.data.ProxySettings,
    onSelect: (mode: String, autoProfileId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = buildList {
        add("direct" to "直连")
        proxy.profiles.forEach { add(it.id to "固定走 ${it.name}") }
        add("auto" to "自动切换（规则 + 默认出口）")
    }
    var autoId by remember(proxy) { mutableStateOf(proxy.autoProfileId.ifEmpty { proxy.profiles.firstOrNull()?.id ?: "" }) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("代理模式") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                options.forEach { (id, label) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(id, autoId) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = proxy.mode == id,
                            onClick = { onSelect(id, autoId) },
                        )
                        Spacer(Modifier.padding(3.dp))
                        Text(label)
                    }
                    // 自动切换时选默认出口
                    if (id == "auto" && proxy.profiles.isNotEmpty()) {
                        Text(
                            "未命中直连规则时走：",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                        proxy.profiles.forEach { pf ->
                            Row(
                                Modifier.fillMaxWidth().clickable { autoId = pf.id }
                                    .padding(start = 28.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                androidx.compose.material3.RadioButton(selected = autoId == pf.id, onClick = { autoId = pf.id })
                                Spacer(Modifier.padding(3.dp))
                                Text(pf.name, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                if (proxy.profiles.isEmpty()) {
                    Text(
                        "还没有代理出口，先到「代理出口」添加",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

/** 出口管理：列表 + 删除 + 添加（名称/类型/主机/端口） */
@Composable
private fun ProxyProfilesDialog(
    proxy: com.felix021.navigateur.data.ProxySettings,
    onSave: (List<com.felix021.navigateur.data.ProxyProfile>) -> Unit,
    onDismiss: () -> Unit,
) {
    var adding by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("HTTP") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (adding) "添加代理出口" else "代理出口") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                if (!adding) {
                    proxy.profiles.forEach { pf ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(pf.name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${pf.type} ${pf.host}:${pf.port}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { onSave(proxy.profiles.filterNot { it.id == pf.id }) }) {
                                Icon(Icons.Filled.Close, contentDescription = "删除", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    TextButton(onClick = { adding = true }) { Text("＋ 添加") }
                } else {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") }, singleLine = true)
                    Spacer(Modifier.padding(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        listOf("HTTP", "SOCKS5").forEach { t ->
                            Row(
                                Modifier.clickable { type = t }.padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                androidx.compose.material3.RadioButton(selected = type == t, onClick = { type = t })
                                Text(t, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("主机") }, singleLine = true)
                    Spacer(Modifier.padding(4.dp))
                    OutlinedTextField(value = port, onValueChange = { port = it.filter { c -> c.isDigit() } }, label = { Text("端口") }, singleLine = true)
                }
            }
        },
        confirmButton = {
            if (adding) {
                TextButton(
                    onClick = {
                        val p = port.toIntOrNull() ?: return@TextButton
                        if (host.isBlank() || p !in 1..65535) return@TextButton
                        onSave(
                            proxy.profiles + com.felix021.navigateur.data.ProxyProfile(
                                id = java.util.UUID.randomUUID().toString(),
                                name = name.ifBlank { host },
                                type = type, host = host.trim(), port = p,
                            ),
                        )
                        adding = false; name = ""; host = ""; port = ""
                    },
                ) { Text("保存") }
            } else {
                TextButton(onClick = onDismiss) { Text("完成") }
            }
        },
        dismissButton = {
            if (adding) TextButton(onClick = { adding = false }) { Text("返回") }
        },
    )
}

/** 规则管理：pattern + 动作（直连/走代理） */
@Composable
private fun ProxyRulesDialog(
    proxy: com.felix021.navigateur.data.ProxySettings,
    onSave: (List<com.felix021.navigateur.data.ProxyRule>) -> Unit,
    onDismiss: () -> Unit,
) {
    var adding by remember { mutableStateOf(false) }
    var pattern by remember { mutableStateOf("") }
    var direct by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (adding) "添加规则" else "自动切换规则") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                if (!adding) {
                    Text(
                        "自动模式下：命中「直连」规则的域名不走代理，其余走默认出口",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    proxy.rules.forEach { r ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "${if (r.action == "direct") "直连" else "代理"}  ${r.pattern}",
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            IconButton(onClick = { onSave(proxy.rules.filterNot { it.pattern == r.pattern }) }) {
                                Icon(Icons.Filled.Close, contentDescription = "删除", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    TextButton(onClick = { adding = true }) { Text("＋ 添加") }
                } else {
                    OutlinedTextField(
                        value = pattern,
                        onValueChange = { pattern = it },
                        label = { Text("域名，如 baidu.com 或 *.google.com") },
                        singleLine = true,
                    )
                    Spacer(Modifier.padding(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.RadioButton(selected = direct, onClick = { direct = true })
                        Text("直连", Modifier.clickable { direct = true })
                        Spacer(Modifier.padding(8.dp))
                        androidx.compose.material3.RadioButton(selected = !direct, onClick = { direct = false })
                        Text("走代理", Modifier.clickable { direct = false })
                    }
                }
            }
        },
        confirmButton = {
            if (adding) {
                TextButton(
                    onClick = {
                        if (pattern.isBlank()) return@TextButton
                        onSave(
                            proxy.rules.filterNot { it.pattern == pattern.trim() } +
                                com.felix021.navigateur.data.ProxyRule(pattern.trim(), if (direct) "direct" else "proxy"),
                        )
                        adding = false; pattern = ""
                    },
                ) { Text("保存") }
            } else {
                TextButton(onClick = onDismiss) { Text("完成") }
            }
        },
        dismissButton = {
            if (adding) TextButton(onClick = { adding = false }) { Text("返回") }
        },
    )
}
