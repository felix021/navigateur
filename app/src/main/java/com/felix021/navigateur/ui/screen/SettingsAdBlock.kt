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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
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
import com.felix021.navigateur.ui.BrowserController

/** 广告拦截二级页：开关 / 更新规则 / 站点白名单 */
@Composable
internal fun AdBlockSettingsPage(controller: BrowserController, onBack: () -> Unit) {
    val settings by controller.container.settings.settings.collectAsState()
    val adStatus by controller.container.adBlock.status.collectAsState()
    var showAllow by remember { mutableStateOf(false) }

    SubPageScaffold("广告拦截", onBack) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()),
        ) {
            SwitchItem(
                title = "拦截广告",
                subtitle = "基于 EasyList + EasyList China，拦截请求并隐藏页面广告元素",
                checked = settings.adBlockEnabled,
                onChange = { enabled ->
                    controller.container.settings.update { it.copy(adBlockEnabled = enabled) }
                },
            )
            SettingItem(
                title = "更新广告规则",
                value = adRulesLabel(adStatus),
            ) { controller.container.adBlock.update() }
            SettingItem(
                title = "站点白名单",
                value = if (settings.adBlockAllowlist.isEmpty()) "无（全部站点生效）"
                else "${settings.adBlockAllowlist.size} 个站点不拦截",
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
