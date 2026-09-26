package com.felix021.navigateur.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.felix021.navigateur.R
import com.felix021.navigateur.data.UaPreset
import com.felix021.navigateur.data.UaPresets

/**
 * UA 预设对话框：内置预设点击即选中；自定义预设支持编辑/删除；
 * 底部按钮新增自定义预设（[onEdit] 传 null 表示新增）。
 */
@Composable
internal fun UaPresetsDialog(
    customPresets: List<UaPreset>,
    selectedId: String,
    onSelect: (UaPreset) -> Unit,
    onEdit: (UaPreset?) -> Unit,
    onDelete: (UaPreset) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_ua)) },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                UaPresets.ALL.forEach { p ->
                    PresetRow(
                        label = p.labelText(),
                        selected = selectedId == p.id,
                        onClick = { onSelect(p) },
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text(
                    stringResource(R.string.ua_presets_section),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                if (customPresets.isEmpty()) {
                    Text(
                        stringResource(R.string.ua_presets_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 6.dp),
                    )
                }
                customPresets.forEach { p ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selectedId == p.id, onClick = { onSelect(p) })
                        Column(
                            Modifier.weight(1f).clickable { onSelect(p) }.padding(vertical = 8.dp),
                        ) {
                            Text(p.label, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                p.ua.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(onClick = { onEdit(p) }) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.edit),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        IconButton(onClick = { onDelete(p) }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.delete),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
                TextButton(onClick = { onEdit(null) }) {
                    Text(stringResource(R.string.ua_presets_add))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) }
        },
    )
}

@Composable
private fun PresetRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.size(4.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

/** 新增/编辑自定义 UA 预设：名称 + UA 字符串，两项都非空才可保存 */
@Composable
internal fun UaPresetEditDialog(
    initial: UaPreset?,
    onDismiss: () -> Unit,
    onOk: (name: String, ua: String) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.label.orEmpty()) }
    var ua by remember { mutableStateOf(initial?.ua.orEmpty()) }
    val valid = name.isNotBlank() && ua.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (initial == null) R.string.ua_presets_add else R.string.ua_preset_edit,
                ),
            )
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.field_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.padding(6.dp))
                OutlinedTextField(
                    value = ua,
                    onValueChange = { ua = it },
                    label = { Text(stringResource(R.string.settings_ua)) },
                    supportingText = { Text(stringResource(R.string.ua_preset_ua_hint)) },
                    minLines = 2,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { onOk(name.trim(), ua.trim()) },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
