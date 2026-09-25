package com.felix021.navigateur.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.felix021.navigateur.data.BrowserSettings
import com.felix021.navigateur.data.ProxyRepository
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

/** 二级设置页 */
private enum class SubPage(val title: String) {
    AdBlock("广告拦截"),
    Proxy("代理"),
    Developer("开发者"),
    Privacy("隐私"),
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(controller: BrowserController) {
    val settings by controller.container.settings.settings.collectAsState()
    var sub by remember { mutableStateOf<SubPage?>(null) }
    // 二级页返回 → 一级页；一级页的返回由 AppRoot 的 BackHandler 接管（回浏览页）
    BackHandler(enabled = sub != null) { sub = null }

    sub?.let { page ->
        when (page) {
            SubPage.AdBlock -> AdBlockSettingsPage(controller) { sub = null }
            SubPage.Proxy -> ProxySettingsPage(controller) { sub = null }
            SubPage.Developer -> DeveloperSettingsPage(controller) { sub = null }
            SubPage.Privacy -> PrivacySettingsPage(controller) { sub = null }
        }
        return
    }

    var showHome by remember { mutableStateOf(false) }
    var showEngine by remember { mutableStateOf(false) }
    var showTheme by remember { mutableStateOf(false) }
    var showFont by remember { mutableStateOf(false) }
    var showUa by remember { mutableStateOf(false) }
    var showUaCustom by remember { mutableStateOf(false) }
    var showLandSide by remember { mutableStateOf(false) }
    val adStatus by controller.container.adBlock.status.collectAsState()
    val proxy = ProxyRepository.fromJson(settings.proxyJson)

    val update: ((BrowserSettings) -> BrowserSettings) -> Unit =
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

            SectionHeader("更多")
            SettingItem(
                title = "广告拦截",
                value = if (settings.adBlockEnabled) adRulesLabel(adStatus)
                else "已关闭 · ${adStatus.ruleCount} 条规则",
            ) { sub = SubPage.AdBlock }
            SettingItem(
                title = "代理",
                value = proxyModeLabel(proxy),
            ) { sub = SubPage.Proxy }
            SettingItem(
                title = "开发者",
                value = if (settings.devTools || settings.remoteDebug) buildList {
                    if (settings.devTools) add("eruda")
                    if (settings.remoteDebug) add("远程调试")
                }.joinToString(" / ") else "页面内工具 / 远程调试",
            ) { sub = SubPage.Developer }
            SettingItem(
                title = "隐私",
                value = "清理浏览数据 / 按站点清理",
            ) { sub = SubPage.Privacy }

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
}

/** 二级页通用壳：标题 + 返回一级 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun SubPageScaffold(title: String, onBack: () -> Unit, content: @Composable (pad: androidx.compose.foundation.layout.PaddingValues) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { pad -> content(pad) }
}

internal fun uaDisplay(s: BrowserSettings): String {
    val preset = UaPresets.byId(s.uaPresetId)
    return if (preset.id == UaPresets.CUSTOM) {
        s.customUserAgent.ifBlank { "默认（本机）" }
    } else {
        preset.label
    }
}

internal fun adRulesLabel(s: com.felix021.navigateur.data.AdBlockStatus): String = when {
    s.updating -> "正在更新…"
    s.lastError != null -> "更新失败：${s.lastError}（点击重试）"
    s.updatedAt > 0 -> {
        val d = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.CHINA).format(java.util.Date(s.updatedAt))
        "${s.ruleCount} 条规则 · $d 更新"
    }
    else -> "${s.ruleCount} 条规则 · 内置版本（点击在线更新）"
}

internal fun proxyModeLabel(p: com.felix021.navigateur.data.ProxySettings): String = when (p.mode) {
    "direct" -> "直连"
    "auto" -> {
        val name = p.profiles.firstOrNull { it.id == p.autoProfileId }?.name ?: "（未设出口）"
        val def = if (p.autoDefault == "direct") "未命中直连" else "未命中走代理"
        "自动切换（出口：$name，$def）"
    }
    else -> p.profiles.firstOrNull { it.id == p.mode }?.let { "固定：${it.name}" } ?: "直连"
}
