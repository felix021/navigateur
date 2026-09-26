package com.felix021.puff.ui.screen

import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatDelegate
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.felix021.puff.R
import com.felix021.puff.ui.accentLabelRes
import com.felix021.puff.data.AdBlockStatus
import com.felix021.puff.data.BrowserSettings
import com.felix021.puff.data.ProxyRepository
import com.felix021.puff.data.ProxySettings
import com.felix021.puff.data.SearchEngines
import com.felix021.puff.data.ThemeMode
import com.felix021.puff.data.UaPreset
import com.felix021.puff.data.UaPresets
import com.felix021.puff.data.resolveUa
import com.felix021.puff.ui.BrowserController
import com.felix021.puff.ui.Screen
import com.felix021.puff.ui.component.SingleChoiceDialog
import com.felix021.puff.ui.component.TextInputDialog
import com.felix021.puff.util.UrlUtils

private val THEME_LABELS = mapOf(
    ThemeMode.FOLLOW_SYSTEM to R.string.follow_system,
    ThemeMode.LIGHT to R.string.theme_light,
    ThemeMode.DARK to R.string.theme_dark,
)

private val FONT_OPTIONS = listOf(
    "" to R.string.font_default,
    "sans-serif" to R.string.font_sans,
    "serif" to R.string.font_serif,
    "monospace" to R.string.font_monospace,
)

/** 界面语言：tag 为 BCP-47 语言标签（"" = 跟随系统）；nativeName 是各语言自称，不随界面翻译 */
private data class LangOption(val tag: String, val nativeName: String?)

private val LANGUAGES = listOf(
    LangOption("", null),
    LangOption("zh", "中文"),
    LangOption("en", "English"),
    LangOption("fr", "Français"),
    LangOption("ja", "日本語"),
    LangOption("ru", "Русский"),
    LangOption("de", "Deutsch"),
    LangOption("es", "Español"),
)

/** 二级设置页 */
internal enum class SubPage(@androidx.annotation.StringRes val titleRes: Int) {
    AdBlock(R.string.entry_adblock),
    Proxy(R.string.entry_proxy),
    Developer(R.string.entry_developer),
    Privacy(R.string.entry_privacy),
    About(R.string.entry_about),
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(controller: BrowserController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settings by controller.container.settings.settings.collectAsState()
    var sub by remember { mutableStateOf<SubPage?>(null) }
    val nav = remember { SettingsNav() }
    // 二级页返回 → 一级页；一级页的返回由 AppRoot 的 BackHandler 接管（回浏览页）
    BackHandler(enabled = sub != null) { sub = null }

    sub?.let { page ->
        // 搜索跳转需要共享导航（坐标上报 / 呼吸灯），二级页同样在作用域内
        androidx.compose.runtime.CompositionLocalProvider(LocalSettingsNav provides nav) {
            when (page) {
                SubPage.AdBlock -> AdBlockSettingsPage(controller) { sub = null }
                SubPage.Proxy -> ProxySettingsPage(controller) { sub = null }
                SubPage.Developer -> DeveloperSettingsPage(controller) { sub = null }
                SubPage.Privacy -> PrivacySettingsPage(controller) { sub = null }
                SubPage.About -> AboutSettingsPage(controller) { sub = null }
            }
        }
        return
    }

    var showHome by remember { mutableStateOf(false) }
    var showEngine by remember { mutableStateOf(false) }
    var showLanguage by remember { mutableStateOf(false) }
    var showTheme by remember { mutableStateOf(false) }
    var showAccent by remember { mutableStateOf(false) }
    var showFont by remember { mutableStateOf(false) }
    var showUa by remember { mutableStateOf(false) }
    var showUaEdit by remember { mutableStateOf(false) }
    var uaEditTarget by remember { mutableStateOf<com.felix021.puff.data.UaPreset?>(null) }
    var showLandSide by remember { mutableStateOf(false) }
    val adStatus by controller.container.adBlock.status.collectAsState()
    val proxy = ProxyRepository.fromJson(settings.proxyJson)

    val update: ((BrowserSettings) -> BrowserSettings) -> Unit =
        { controller.container.settings.update(it) }

    // 当前语言：empty = 跟随系统；取主语言与选项表对齐（zh-CN → zh）
    val appliedLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = LANGUAGES.firstOrNull {
        it.tag.isNotEmpty() && appliedLocales.takeIf { l -> !l.isEmpty }
            ?.get(0)?.language == it.tag
    } ?: LANGUAGES[0]

    val listScroll = rememberScrollState()
    androidx.compose.runtime.CompositionLocalProvider(LocalSettingsNav provides nav) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = { controller.screen.value = Screen.Browser }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            SettingsSearchBar { entry -> nav.requestJump(entry) { p -> sub = p } }
            SettingsJumpTarget(listScroll, null)
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(listScroll)
                    .settingsList(),
            ) {
            SectionHeader(stringResource(R.string.section_general))
            SettingItem(
                title = stringResource(R.string.settings_homepage),
                entryId = "s.homepage",
                value = if (UrlUtils.isHome(settings.homepage)) stringResource(R.string.homepage_builtin)
                else settings.homepage,
            ) { showHome = true }
            SettingItem(
                title = stringResource(R.string.settings_search_engine),
                entryId = "s.search_engine",
                value = SearchEngines.byId(settings.searchEngineId).nameText(),
            ) { showEngine = true }
            SettingItem(
                title = stringResource(R.string.settings_language),
                entryId = "s.language",
                value = currentLang.nativeName ?: stringResource(R.string.follow_system),
            ) { showLanguage = true }
            SwitchItem(
                title = stringResource(R.string.settings_save_passwords),
                entryId = "s.save_passwords",
                checked = settings.savePasswords,
                onChange = { enabled -> update { it.copy(savePasswords = enabled) } },
            )

            SectionHeader(stringResource(R.string.section_display))
            SettingItem(
                title = stringResource(R.string.settings_theme),
                entryId = "s.theme",
                value = stringResource(THEME_LABELS[settings.themeMode] ?: R.string.follow_system),
            ) { showTheme = true }
            SettingItem(
                title = stringResource(R.string.settings_accent),
                entryId = "s.accent",
                value = if (settings.accent == "custom") stringResource(R.string.accent_custom)
                else stringResource(accentLabelRes(settings.accent)),
            ) { showAccent = true }
            SettingsItemShell("s.zoom") {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.settings_zoom), style = MaterialTheme.typography.bodyLarge)
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
            }
            SettingItem(
                title = stringResource(R.string.settings_font),
                entryId = "s.font",
                value = FONT_OPTIONS.firstOrNull { it.first == settings.fontFamily }
                    ?.let { stringResource(it.second) }.orEmpty(),
            ) { showFont = true }
            SettingItem(
                title = stringResource(R.string.settings_landscape_side),
                entryId = "s.land_side",
                value = if (settings.landscapeToolbarSide == "left") stringResource(R.string.landscape_left)
                else stringResource(R.string.landscape_right),
            ) { showLandSide = true }
            SwitchItem(
                title = stringResource(R.string.settings_landscape_fullscreen),
                entryId = "s.land_fullscreen",
                subtitle = stringResource(R.string.landscape_fullscreen_desc),
                checked = settings.landscapeFullscreen,
                onChange = { enabled -> update { it.copy(landscapeFullscreen = enabled) } },
            )
            SwitchItem(
                title = stringResource(R.string.settings_landscape_safe_area),
                entryId = "s.land_safe",
                subtitle = stringResource(R.string.landscape_safe_area_desc),
                checked = settings.landscapeBottomSafeArea,
                onChange = { enabled -> update { it.copy(landscapeBottomSafeArea = enabled) } },
            )

            SectionHeader(stringResource(R.string.section_website))
            SwitchItem(
                title = stringResource(R.string.settings_desktop_default),
                entryId = "s.desktop_default",
                subtitle = stringResource(R.string.desktop_default_desc),
                checked = settings.desktopModeDefault,
                onChange = { enabled -> update { it.copy(desktopModeDefault = enabled) } },
            )
            SettingItem(
                title = stringResource(R.string.settings_ua),
                entryId = "s.ua",
                value = uaDisplay(settings),
            ) { showUa = true }

            SectionHeader(stringResource(R.string.section_more))
            SettingItem(
                title = stringResource(R.string.entry_adblock),
                entryId = "s.adblock",
                value = if (settings.adBlockEnabled) adRulesLabel(adStatus)
                else stringResource(R.string.adblock_off_status, adStatus.ruleCount),
            ) { sub = SubPage.AdBlock }
            SettingItem(
                title = stringResource(R.string.entry_proxy),
                entryId = "s.proxy",
                value = proxyModeLabel(proxy),
            ) { sub = SubPage.Proxy }
            SettingItem(
                title = stringResource(R.string.entry_developer),
                entryId = "s.dev",
                value = if (settings.devTools || settings.remoteDebug) buildList {
                    if (settings.devTools) add("eruda")
                    if (settings.remoteDebug) add(stringResource(R.string.developer_remote_on))
                }.joinToString(" / ") else stringResource(R.string.developer_summary),
            ) { sub = SubPage.Developer }
            SettingItem(
                title = stringResource(R.string.entry_privacy),
                entryId = "s.privacy",
                value = stringResource(R.string.privacy_summary),
            ) { sub = SubPage.Privacy }
            SettingItem(
                title = stringResource(R.string.entry_about),
                value = aboutVersion(context),
                entryId = "s.about",
            ) { sub = SubPage.About }

            Spacer(Modifier.height(32.dp))
            }
        }
    }
    } // CompositionLocalProvider

    if (showHome) {
        TextInputDialog(
            title = stringResource(R.string.settings_homepage),
            initial = settings.homepage,
            label = stringResource(R.string.field_url),
            supportingText = stringResource(R.string.homepage_hint),
            onDismiss = { showHome = false },
            onOk = { v ->
                update { it.copy(homepage = v.ifBlank { UrlUtils.HOME }) }
                showHome = false
            },
        )
    }
    if (showEngine) {
        SingleChoiceDialog(
            title = stringResource(R.string.settings_search_engine),
            options = SearchEngines.ALL,
            selected = SearchEngines.byId(settings.searchEngineId),
            label = { it.nameText() },
            onDismiss = { showEngine = false },
            onSelect = {
                update { s -> s.copy(searchEngineId = it.id) }
                showEngine = false
            },
        )
    }
    if (showLanguage) {
        SingleChoiceDialog(
            title = stringResource(R.string.settings_language),
            options = LANGUAGES,
            selected = currentLang,
            label = { it.nativeName ?: stringResource(R.string.follow_system) },
            onDismiss = { showLanguage = false },
            onSelect = { opt ->
                AppCompatDelegate.setApplicationLocales(
                    if (opt.tag.isEmpty()) LocaleListCompat.getEmptyLocaleList()
                    else LocaleListCompat.forLanguageTags(opt.tag),
                )
                showLanguage = false
            },
        )
    }
    if (showAccent) {
        AccentDialog(
            currentAccent = settings.accent,
            currentHue = settings.accentHue,
            dark = com.felix021.puff.ui.isDarkTheme(settings.themeMode),
            onDismiss = { showAccent = false },
            onSelect = { key, hue -> update { s -> s.copy(accent = key, accentHue = hue) } },
        )
    }
    if (showTheme) {
        SingleChoiceDialog(
            title = stringResource(R.string.settings_theme),
            options = ThemeMode.entries.toList(),
            selected = settings.themeMode,
            label = { stringResource(THEME_LABELS[it] ?: R.string.follow_system) },
            onDismiss = { showTheme = false },
            onSelect = {
                update { s -> s.copy(themeMode = it) }
                showTheme = false
            },
        )
    }
    if (showFont) {
        SingleChoiceDialog(
            title = stringResource(R.string.settings_font),
            options = FONT_OPTIONS,
            selected = FONT_OPTIONS.firstOrNull { it.first == settings.fontFamily },
            label = { stringResource(it.second) },
            onDismiss = { showFont = false },
            onSelect = {
                update { s -> s.copy(fontFamily = it.first) }
                showFont = false
            },
        )
    }
    if (showLandSide) {
        SingleChoiceDialog(
            title = stringResource(R.string.settings_landscape_side),
            options = listOf("left", "right"),
            selected = settings.landscapeToolbarSide,
            label = { if (it == "left") stringResource(R.string.landscape_left) else stringResource(R.string.landscape_right) },
            onDismiss = { showLandSide = false },
            onSelect = {
                update { s -> s.copy(landscapeToolbarSide = it) }
                showLandSide = false
            },
        )
    }
    if (showUa) {
        UaPresetsDialog(
            customPresets = settings.customUas,
            selectedId = settings.uaPresetId,
            onSelect = { p -> update { s -> s.copy(uaPresetId = p.id) } },
            onEdit = { p -> uaEditTarget = p; showUaEdit = true },
            onDelete = { p ->
                update { s -> s.copy(
                    customUas = s.customUas.filterNot { it.id == p.id },
                    uaPresetId = if (s.uaPresetId == p.id) UaPresets.byId("default").id else s.uaPresetId,
                ) }
            },
            onDismiss = { showUa = false },
        )
    }
    if (showUaEdit) {
        UaPresetEditDialog(
            initial = uaEditTarget,
            onDismiss = { showUaEdit = false },
            onOk = { name, ua ->
                update { s ->
                    val target = uaEditTarget
                    if (target == null) {
                        val np = com.felix021.puff.data.UaPreset("cus_${System.currentTimeMillis()}", name, ua)
                        s.copy(customUas = s.customUas + np, uaPresetId = np.id)
                    } else {
                        s.copy(customUas = s.customUas.map {
                            if (it.id == target.id) it.copy(label = name, ua = ua) else it
                        })
                    }
                }
                showUaEdit = false
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { pad -> content(pad) }
}

/** 关于页入口摘要：v<versionName>（读失败显示空） */
@Composable
internal fun aboutVersion(context: android.content.Context): String = remember(context) {
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
        .getOrNull()?.let { "v$it" }.orEmpty()
}

@Composable
internal fun uaDisplay(s: BrowserSettings): String = s.resolveUa().labelText()

@Composable
internal fun adRulesLabel(s: AdBlockStatus): String = when {
    s.updating -> stringResource(R.string.adblock_updating)
    s.lastError != null -> stringResource(R.string.adblock_update_failed, s.lastError)
    s.updatedAt > 0 -> {
        val d = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(s.updatedAt))
        stringResource(R.string.adblock_rules_updated, s.ruleCount, d)
    }
    else -> stringResource(R.string.adblock_rules_builtin, s.ruleCount)
}

@Composable
internal fun proxyModeLabel(p: ProxySettings): String = when (p.mode) {
    "direct" -> stringResource(R.string.label_direct)
    "auto" -> {
        val name = p.profiles.firstOrNull { it.id == p.autoProfileId }?.name
            ?: stringResource(R.string.proxy_outlet_unset)
        val def = if (p.autoDefault == "direct") stringResource(R.string.proxy_default_direct)
        else stringResource(R.string.proxy_default_proxy)
        stringResource(R.string.proxy_status_auto, name, def)
    }
    else -> p.profiles.firstOrNull { it.id == p.mode }?.let { stringResource(R.string.proxy_status_fixed, it.name) }
        ?: stringResource(R.string.label_direct)
}
