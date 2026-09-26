package com.felix021.puff.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.felix021.puff.R
import com.felix021.puff.ui.ACCENTS
import com.felix021.puff.ui.accentColorScheme
import com.felix021.puff.ui.component.AppDialog

/**
 * 主题色选择，交互对齐 feelime「键盘色调」色板：
 * 横向 swatch 圆点单选（点击即生效，选中态描边+放大），下方色名确认；
 * 自定义项附色相滑块（拖动即时预览，松手落盘）。
 */
@Composable
internal fun AccentDialog(
    currentAccent: String,
    currentHue: Float,
    dark: Boolean,
    onDismiss: () -> Unit,
    onSelect: (String, Float) -> Unit,
) {
    var selKey by remember { mutableStateOf(currentAccent) }
    var selHue by remember { mutableFloatStateOf(currentHue) }

    val preview = accentColorScheme(selKey, selHue, dark).primary
    val isCustom = selKey == "custom"
    val labelRes = if (isCustom) R.string.accent_custom
    else ACCENTS.firstOrNull { it.key == selKey }?.labelRes ?: R.string.accent_custom

    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_accent)) },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // swatch 行：6 预设 + 自定义
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ACCENTS.forEach { opt ->
                        val selected = selKey == opt.key
                        Swatch(
                            color = accentColorScheme(opt.key, opt.hue, dark).primary,
                            selected = selected,
                            onClick = {
                                selKey = opt.key
                                selHue = opt.hue
                                onSelect(opt.key, opt.hue)
                            },
                        )
                    }
                    Swatch(
                        color = if (isCustom) preview else Color.hsl(selHue, 0.62f, 0.45f),
                        selected = isCustom,
                        onClick = {
                            selKey = "custom"
                            onSelect("custom", selHue)
                        },
                    )
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(labelRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (isCustom) {
                    Spacer(Modifier.height(6.dp))
                    Slider(
                        value = selHue,
                        onValueChange = { selHue = it },
                        onValueChangeFinished = { onSelect("custom", selHue) },
                        valueRange = 0f..360f,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(preview)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.accent_custom_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) }
        },
    )
}

/** 单个色点：选中态用主色描边并放大 12%（feelime 色板同款反馈） */
@Composable
private fun Swatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(34.dp)
            .graphicsLayer {
                scaleX = if (selected) 1.12f else 1f
                scaleY = if (selected) 1.12f else 1f
            }
            .clip(CircleShape)
            .background(color)
            .border(
                width = 2.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else Color.Transparent,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
