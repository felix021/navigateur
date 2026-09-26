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
import com.felix021.navigateur.ui.component.AppDialog
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
import androidx.compose.ui.res.stringResource
import com.felix021.navigateur.R

/** 隐私二级页：清理浏览数据 / 按站点清理 */
@Composable
internal fun PrivacySettingsPage(controller: BrowserController, onBack: () -> Unit) {
    var showClear by remember { mutableStateOf(false) }
    var showSiteClear by remember { mutableStateOf(false) }

    val scroll = rememberScrollState()
    SubPageScaffold(stringResource(R.string.entry_privacy), onBack) { pad ->
        SettingsJumpTarget(scroll, SubPage.Privacy)
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(scroll).settingsList(),
        ) {
            SettingItem(
                title = stringResource(R.string.privacy_clear_all),
                value = stringResource(R.string.privacy_clear_all_desc),
                entryId = "pv.clear_all",
            ) { showClear = true }
            SettingItem(
                title = stringResource(R.string.privacy_clear_site),
                value = stringResource(R.string.privacy_clear_site_desc),
                entryId = "pv.clear_site",
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
    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.privacy_clear_site)) },
        text = {
            Column(
                Modifier
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it.trim() },
                    label = { Text(stringResource(R.string.field_site_domain)) },
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
                CheckRow(stringResource(R.string.clear_data_storage), clearStorage) { clearStorage = it }
                CheckRow(stringResource(R.string.clear_data_passwords), clearPasswords) { clearPasswords = it }
                CheckRow(stringResource(R.string.clear_data_history_site), clearHistory) { clearHistory = it }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (host.isNotBlank()) onClear(host, clearCookies, clearStorage, clearPasswords, clearHistory)
                },
                enabled = host.isNotBlank(),
            ) { Text(stringResource(R.string.clear)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
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
        stringResource(R.string.clear_data_storage),
        stringResource(R.string.clear_data_cache),
        stringResource(R.string.clear_data_form),
        stringResource(R.string.clear_data_passwords),
        stringResource(R.string.clear_data_history),
    )
    val checked = remember { mutableStateListOf(true, true, true, true, false, true) }
    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.privacy_clear_all)) },
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
            }) { Text(stringResource(R.string.clear)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
