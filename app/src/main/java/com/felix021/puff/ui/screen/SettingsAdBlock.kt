package com.felix021.puff.ui.screen

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
import com.felix021.puff.ui.component.AppDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.felix021.puff.ui.BrowserController
import androidx.compose.ui.res.stringResource
import com.felix021.puff.R

/** 广告拦截二级页：开关 / 更新规则 / 站点白名单 */
@Composable
internal fun AdBlockSettingsPage(controller: BrowserController, onBack: () -> Unit) {
    val settings by controller.container.settings.settings.collectAsState()
    val adStatus by controller.container.adBlock.status.collectAsState()
    var showAllow by remember { mutableStateOf(false) }

    val scroll = rememberScrollState()
    SubPageScaffold(stringResource(R.string.entry_adblock), onBack) { pad ->
        SettingsJumpTarget(scroll, SubPage.AdBlock)
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(scroll).settingsList(),
        ) {
            SwitchItem(
                title = stringResource(R.string.adblock_switch),
                entryId = "a.switch",
                subtitle = stringResource(R.string.adblock_switch_desc),
                checked = settings.adBlockEnabled,
                onChange = { enabled ->
                    controller.container.settings.update { it.copy(adBlockEnabled = enabled) }
                },
            )
            SettingItem(
                title = stringResource(R.string.adblock_update),
                value = adRulesLabel(adStatus),
                entryId = "a.update",
            ) { controller.container.adBlock.update() }
            SettingItem(
                entryId = "a.allowlist",
                title = stringResource(R.string.adblock_allowlist),
                value = if (settings.adBlockAllowlist.isEmpty()) stringResource(R.string.adblock_allowlist_empty)
                else stringResource(R.string.adblock_allowlist_count, settings.adBlockAllowlist.size),
            ) { showAllow = true }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showAllow) {
        AdAllowlistDialog(
            hosts = settings.adBlockAllowlist,
            onRemove = { host ->
                controller.container.settings.update { s -> s.copy(adBlockAllowlist = s.adBlockAllowlist - host) }
            },
            onDismiss = { showAllow = false },
        )
    }
}

/** 广告拦截站点白名单管理 */
@Composable
private fun AdAllowlistDialog(
    hosts: Set<String>,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.adblock_allowlist)) },
        text = {
            Column {
                if (hosts.isEmpty()) {
                    Text(
                        stringResource(R.string.adblock_allowlist_hint),
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
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.remove), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } },
    )
}
