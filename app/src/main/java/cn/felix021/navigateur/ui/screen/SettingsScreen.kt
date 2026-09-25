package cn.felix021.navigateur.ui.screen

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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import cn.felix021.navigateur.data.SearchEngines
import cn.felix021.navigateur.data.ThemeMode
import cn.felix021.navigateur.ui.BrowserController
import cn.felix021.navigateur.ui.Screen
import cn.felix021.navigateur.ui.component.SingleChoiceDialog
import cn.felix021.navigateur.ui.component.TextInputDialog
import cn.felix021.navigateur.util.UrlUtils

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
    var showClear by remember { mutableStateOf(false) }

    val update: ((cn.felix021.navigateur.data.BrowserSettings) -> cn.felix021.navigateur.data.BrowserSettings) -> Unit =
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
                    Text("字号", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${settings.textZoomPercent}%",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Slider(
                    value = settings.textZoomPercent.toFloat(),
                    onValueChange = { update { s -> s.copy(textZoomPercent = it.toInt().coerceIn(50, 200)) } },
                    valueRange = 50f..200f,
                )
            }
            SettingItem(
                title = "字体",
                value = FONT_OPTIONS.firstOrNull { it.first == settings.fontFamily }?.second.orEmpty(),
            ) { showFont = true }

            SectionHeader("网站")
            SwitchItem(
                title = "默认桌面模式",
                subtitle = "新标签页默认以桌面 UA 打开",
                checked = settings.desktopModeDefault,
                onChange = { enabled -> update { it.copy(desktopModeDefault = enabled) } },
            )
            SettingItem(
                title = "自定义 User-Agent",
                value = settings.customUserAgent.ifBlank { "默认（移动端）" },
            ) { showUa = true }

            SectionHeader("隐私")
            SettingItem(
                title = "清理浏览数据",
                value = "Cookie / 站点存储 / 缓存 / 密码",
            ) { showClear = true }

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
    if (showUa) {
        TextInputDialog(
            title = "自定义 User-Agent",
            initial = settings.customUserAgent,
            label = "UA 字符串",
            supportingText = "留空使用默认移动端 UA；立即生效并刷新已打开页面",
            onDismiss = { showUa = false },
            onOk = { v ->
                update { it.copy(customUserAgent = v) }
                showUa = false
            },
        )
    }
    if (showClear) {
        ClearDataDialog(
            onDismiss = { showClear = false },
            onClear = { cookies, storage, cache, form, pw ->
                controller.clearData(cookies, storage, cache, form, pw)
                showClear = false
            },
        )
    }
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
    onClear: (cookies: Boolean, storage: Boolean, cache: Boolean, formData: Boolean, passwords: Boolean) -> Unit,
) {
    val labels = listOf(
        "Cookie",
        "站点存储（localStorage 等）",
        "缓存",
        "表单数据",
        "已保存密码",
    )
    val checked = remember { mutableStateListOf(true, true, true, true, false) }
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
                onClear(checked[0], checked[1], checked[2], checked[3], checked[4])
            }) { Text("清理") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
