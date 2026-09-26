package com.felix021.navigateur.ui.screen

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.felix021.navigateur.data.ProxyProfile
import com.felix021.navigateur.data.ProxyRepository
import com.felix021.navigateur.data.ProxyRule
import com.felix021.navigateur.data.ProxyRuleParseResult
import com.felix021.navigateur.data.ProxySettings
import com.felix021.navigateur.ui.BrowserController
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.res.stringResource
import com.felix021.navigateur.R

/** 代理二级页：模式 / 出口 / 规则（含 AutoProxy list 导入） */
@Composable
internal fun ProxySettingsPage(controller: BrowserController, onBack: () -> Unit) {
    val settings by controller.container.settings.settings.collectAsState()
    var showMode by remember { mutableStateOf(false) }
    var showProfiles by remember { mutableStateOf(false) }
    var showRules by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    val proxy = ProxyRepository.fromJson(settings.proxyJson)

    fun save(transform: (ProxySettings) -> ProxySettings) {
        controller.container.settings.update { s ->
            s.copy(proxyJson = ProxyRepository.toJson(transform(ProxyRepository.fromJson(s.proxyJson))))
        }
    }

    SubPageScaffold(stringResource(R.string.entry_proxy), onBack) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()),
        ) {
            SettingItem(title = stringResource(R.string.proxy_current_mode), value = proxyModeLabel(proxy)) { showMode = true }
            SettingItem(
                title = stringResource(R.string.proxy_outlets),
                value = if (proxy.profiles.isEmpty()) stringResource(R.string.proxy_not_added)
                else proxy.profiles.joinToString(" / ") { it.name },
            ) { showProfiles = true }
            SettingItem(
                title = stringResource(R.string.proxy_rules_title),
                value = if (proxy.rules.isEmpty()) stringResource(R.string.proxy_not_added)
                else stringResource(
                    R.string.proxy_rules_count, proxy.rules.size,
                    proxy.rules.count { it.action == "direct" },
                    proxy.rules.count { it.action == "proxy" },
                ),
            ) { showRules = true }
            SettingItem(
                title = stringResource(R.string.proxy_import_entry),
                value = stringResource(R.string.proxy_import_entry_desc),
            ) { showImport = true }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showMode) {
        ProxyModeDialog(
            proxy = proxy,
            onSelect = { mode, autoId, autoDefault ->
                save { it.copy(mode = mode, autoProfileId = autoId, autoDefault = autoDefault) }
                showMode = false
            },
            onDismiss = { showMode = false },
        )
    }
    if (showProfiles) {
        ProxyProfilesDialog(
            proxy = proxy,
            onSave = { newList -> save { it.copy(profiles = newList) } },
            onDismiss = { showProfiles = false },
        )
    }
    if (showRules) {
        ProxyRulesDialog(
            proxy = proxy,
            onSave = { newRules -> save { it.copy(rules = newRules) } },
            onDismiss = { showRules = false },
        )
    }
    if (showImport) {
        ProxyImportDialog(
            existing = proxy.rules,
            onImport = { newRules, autoDefault ->
                save { it.copy(rules = newRules, autoDefault = autoDefault) }
                showImport = false
            },
            onDismiss = { showImport = false },
        )
    }
}

// ---------- 模式选择 ----------

/** 模式选择：直连 / 各出口 / 自动切换（出口 + 未命中默认走向） */
@Composable
private fun ProxyModeDialog(
    proxy: ProxySettings,
    onSelect: (mode: String, autoProfileId: String, autoDefault: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = buildList {
        add("direct" to stringResource(R.string.label_direct))
        proxy.profiles.forEach { add(it.id to stringResource(R.string.proxy_mode_fixed_route, it.name)) }
        add("auto" to stringResource(R.string.proxy_mode_auto_rules))
    }
    var autoId by remember(proxy) { mutableStateOf(proxy.autoProfileId.ifEmpty { proxy.profiles.firstOrNull()?.id ?: "" }) }
    var autoDefault by remember(proxy) { mutableStateOf(proxy.autoDefault) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.proxy_mode_title)) },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                options.forEach { (id, label) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(id, autoId, autoDefault) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = proxy.mode == id,
                            onClick = { onSelect(id, autoId, autoDefault) },
                        )
                        Spacer(Modifier.padding(3.dp))
                        Text(label)
                    }
                    if (id == "auto" && proxy.profiles.isNotEmpty()) {
                        Text(
                            stringResource(R.string.proxy_default_outlet_hint),
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
                                RadioButton(selected = autoId == pf.id, onClick = { autoId = pf.id })
                                Spacer(Modifier.padding(3.dp))
                                Text(pf.name, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Text(
                            stringResource(R.string.proxy_unmatched_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 12.dp, top = 6.dp),
                        )
                        listOf(
                            "proxy" to stringResource(R.string.proxy_unmatched_proxy),
                            "direct" to stringResource(R.string.proxy_unmatched_direct),
                        ).forEach { (v, t) ->
                            Row(
                                Modifier.fillMaxWidth().clickable { autoDefault = v }
                                    .padding(start = 28.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = autoDefault == v, onClick = { autoDefault = v })
                                Spacer(Modifier.padding(3.dp))
                                Text(t, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                if (proxy.profiles.isEmpty()) {
                    Text(
                        stringResource(R.string.proxy_no_outlet_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

// ---------- 出口管理 ----------

/** 出口管理：列表 + 删除 + 添加（名称/类型/主机/端口） */
@Composable
private fun ProxyProfilesDialog(
    proxy: ProxySettings,
    onSave: (List<ProxyProfile>) -> Unit,
    onDismiss: () -> Unit,
) {
    var adding by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("HTTP") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (adding) stringResource(R.string.proxy_add_outlet) else stringResource(R.string.proxy_outlets)) },
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
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.delete), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    TextButton(onClick = { adding = true }) { Text(stringResource(R.string.add_prefixed)) }
                } else {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.field_name)) }, singleLine = true)
                    Spacer(Modifier.padding(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        listOf("HTTP", "SOCKS5").forEach { t ->
                            Row(
                                Modifier.clickable { type = t }.padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = type == t, onClick = { type = t })
                                Text(t, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text(stringResource(R.string.field_host)) }, singleLine = true)
                    Spacer(Modifier.padding(4.dp))
                    OutlinedTextField(value = port, onValueChange = { port = it.filter { c -> c.isDigit() } }, label = { Text(stringResource(R.string.field_port)) }, singleLine = true)
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
                            proxy.profiles + ProxyProfile(
                                id = java.util.UUID.randomUUID().toString(),
                                name = name.ifBlank { host },
                                type = type, host = host.trim(), port = p,
                            ),
                        )
                        adding = false; name = ""; host = ""; port = ""
                    },
                ) { Text(stringResource(R.string.save)) }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) }
            }
        },
        dismissButton = {
            if (adding) TextButton(onClick = { adding = false }) { Text(stringResource(R.string.back)) }
        },
    )
}

// ---------- 规则管理 ----------

/** 规则管理：pattern + 动作（直连/走代理） */
@Composable
private fun ProxyRulesDialog(
    proxy: ProxySettings,
    onSave: (List<ProxyRule>) -> Unit,
    onDismiss: () -> Unit,
) {
    var adding by remember { mutableStateOf(false) }
    var pattern by remember { mutableStateOf("") }
    var direct by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (adding) stringResource(R.string.proxy_add_rule) else stringResource(R.string.proxy_rules_title)) },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                if (!adding) {
                    Text(
                        stringResource(R.string.proxy_rules_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    proxy.rules.forEach { r ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "${stringResource(if (r.action == "direct") R.string.label_direct else R.string.label_proxy)}  ${r.pattern}",
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            IconButton(onClick = { onSave(proxy.rules.filterNot { it.pattern == r.pattern }) }) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.delete), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    TextButton(onClick = { adding = true }) { Text(stringResource(R.string.add_prefixed)) }
                } else {
                    OutlinedTextField(
                        value = pattern,
                        onValueChange = { pattern = it },
                        label = { Text(stringResource(R.string.proxy_rule_pattern_label)) },
                        singleLine = true,
                    )
                    Spacer(Modifier.padding(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = direct, onClick = { direct = true })
                        Text(stringResource(R.string.label_direct), Modifier.clickable { direct = true })
                        Spacer(Modifier.padding(8.dp))
                        RadioButton(selected = !direct, onClick = { direct = false })
                        Text(stringResource(R.string.label_via_proxy), Modifier.clickable { direct = false })
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
                                ProxyRule(pattern.trim(), if (direct) "direct" else "proxy"),
                        )
                        adding = false; pattern = ""
                    },
                ) { Text(stringResource(R.string.save)) }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) }
            }
        },
        dismissButton = {
            if (adding) TextButton(onClick = { adding = false }) { Text(stringResource(R.string.back)) }
        },
    )
}

// ---------- 规则列表导入 ----------

/**
 * 导入 AutoProxy / ABP 风格规则列表：粘贴文本或 URL 下载。
 * 导入方式：追加（合并去重）/ 替换（清空后导入）。
 */
@Composable
private fun ProxyImportDialog(
    existing: List<ProxyRule>,
    onImport: (List<ProxyRule>, autoDefault: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var replace by remember { mutableStateOf(false) }
    // gfwlist 是「走代理」清单 → 默认切到直连优先语义，否则导入无意义
    var autoDefault by remember { mutableStateOf("direct") }
    var loading by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<ProxyRuleParseResult?>(null) }

    fun parse(content: String) {
        val r = ProxyRepository.parseRules(content)
        if (r.rules.isEmpty()) {
            Toast.makeText(context, context.getString(R.string.proxy_import_no_rules), Toast.LENGTH_SHORT).show()
        } else {
            preview = r
        }
    }

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text(stringResource(R.string.proxy_import_entry)) },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; preview = null },
                    label = { Text(stringResource(R.string.proxy_import_paste_hint)) },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.padding(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it; preview = null },
                        label = { Text(stringResource(R.string.proxy_import_url_hint)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        enabled = !loading && url.isNotBlank(),
                        onClick = {
                            loading = true
                            scope.launch {
                                try {
                                    val content = withContext(Dispatchers.IO) {
                                        val conn = URL(url.trim()).openConnection() as HttpURLConnection
                                        conn.connectTimeout = 15000
                                        conn.readTimeout = 30000
                                        try {
                                            if (conn.responseCode !in 200..299) {
                                                throw IllegalStateException("HTTP ${conn.responseCode}")
                                            }
                                            conn.inputStream.bufferedReader().readText()
                                        } finally {
                                            conn.disconnect()
                                        }
                                    }
                                    text = content
                                    parse(content)
                                } catch (e: Exception) {
                                    Toast.makeText(context, context.getString(R.string.proxy_import_download_failed, e.message), Toast.LENGTH_LONG).show()
                                } finally {
                                    loading = false
                                }
                            }
                        },
                    ) { Text(if (loading) stringResource(R.string.downloading) else stringResource(R.string.download)) }
                }
                Spacer(Modifier.padding(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = !replace, onClick = { replace = false })
                    Text(stringResource(R.string.import_append), Modifier.clickable { replace = false })
                    Spacer(Modifier.padding(10.dp))
                    RadioButton(selected = replace, onClick = { replace = true })
                    Text(stringResource(R.string.import_replace), Modifier.clickable { replace = true })
                }
                Text(
                    stringResource(R.string.proxy_import_default_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = autoDefault == "direct", onClick = { autoDefault = "direct" })
                    Text(stringResource(R.string.proxy_default_direct), Modifier.clickable { autoDefault = "direct" })
                    Spacer(Modifier.padding(10.dp))
                    RadioButton(selected = autoDefault == "proxy", onClick = { autoDefault = "proxy" })
                    Text(stringResource(R.string.proxy_default_proxy), Modifier.clickable { autoDefault = "proxy" })
                }
                TextButton(
                    enabled = text.isNotBlank() && !loading,
                    onClick = { parse(text) },
                ) { Text(stringResource(R.string.parse_preview)) }
                preview?.let { r ->
                    val directCount = r.rules.count { it.action == "direct" }
                    val proxyCount = r.rules.size - directCount
                    Text(
                        stringResource(
                            R.string.proxy_import_parsed, r.rules.size, directCount, proxyCount,
                            if (r.skipped > 0) stringResource(R.string.proxy_import_skipped, r.skipped) else "",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (replace) {
                        Text(
                            stringResource(R.string.proxy_import_will_clear, existing.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = preview != null && !loading,
                onClick = {
                    val r = preview ?: return@TextButton
                    val merged = if (replace) {
                        r.rules
                    } else {
                        val map = LinkedHashMap<String, ProxyRule>()
                        existing.forEach { map.putIfAbsent(it.pattern, it) }
                        r.rules.forEach { map.putIfAbsent(it.pattern, it) }
                        map.values.toList()
                    }
                    onImport(merged, autoDefault)
                },
            ) { Text(stringResource(R.string.import_action)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
