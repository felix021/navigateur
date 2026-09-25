package com.felix021.navigateur.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.felix021.navigateur.ui.BrowserController

/** 隐私二级页：清理浏览数据 / 按站点清理 */
@Composable
internal fun PrivacySettingsPage(controller: BrowserController, onBack: () -> Unit) {
    var showClear by remember { mutableStateOf(false) }
    var showSiteClear by remember { mutableStateOf(false) }

    SubPageScaffold("隐私", onBack) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()),
        ) {
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
                OutlinedTextField(
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
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label, style = MaterialTheme.typography.bodyMedium)
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
