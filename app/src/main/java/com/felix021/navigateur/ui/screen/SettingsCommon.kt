package com.felix021.navigateur.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.felix021.navigateur.data.SearchEngine
import com.felix021.navigateur.data.UaPreset

/** 设置页共享导航状态（见 SettingsSearch.kt）；null 表示不在设置页环境 */
internal val LocalSettingsNav = staticCompositionLocalOf<SettingsNav?> { null }

/** 内置 UA 预设显示名（有资源走资源，自定义预设用原文） */
@Composable
internal fun UaPreset.labelText(): String = labelRes?.let { stringResource(it) } ?: label

/** 搜索引擎显示名（品牌名不翻译） */
@Composable
internal fun SearchEngine.nameText(): String = nameRes?.let { stringResource(it) } ?: name

/**
 * 设置项外壳：把整块区域的坐标上报给导航（供搜索跳转定位），
 * 搜索命中时整块背景呼吸 3 次提醒位置。
 */
@Composable
internal fun SettingsItemShell(entryId: String?, content: @Composable () -> Unit) {
    val nav = LocalSettingsNav.current
    val highlighted = entryId != null && nav?.highlightId == entryId
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(highlighted) {
        if (highlighted) {
            repeat(3) {
                alpha.animateTo(0.28f, tween(260))
                alpha.animateTo(0f, tween(260))
            }
            nav?.clearHighlight()
        } else {
            alpha.snapTo(0f)
        }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .then(
                if (entryId != null) {
                    Modifier.onGloballyPositioned { coords ->
                        nav?.positions?.put(entryId, coords.positionInRoot().y.toInt())
                    }
                } else Modifier,
            )
            .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha.value)),
    ) { content() }
}

/** 设置项分组标题 */
@Composable
internal fun SectionHeader(title: String) {
    Text(
        title,
        Modifier.fillMaxWidth().padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleSmall,
    )
}

/** 可点击设置项：标题 + 摘要 + 右箭头 */
@Composable
internal fun SettingItem(title: String, value: String, entryId: String? = null, onClick: () -> Unit) {
    SettingsItemShell(entryId) {
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
}

/** 开关设置项 */
@Composable
internal fun SwitchItem(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    entryId: String? = null,
    onChange: (Boolean) -> Unit,
) {
    SettingsItemShell(entryId) {
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
}
