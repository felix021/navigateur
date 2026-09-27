@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.felix021.puff.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
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
                // 预设色板：6 色块一行（6×34dp + 5×6dp = 234dp ≤ 248dp 内容宽）
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                }

                Spacer(Modifier.height(10.dp))
                if (!isCustom) {
                    Text(
                        stringResource(labelRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(6.dp))
                // 自定义独立成行（带文字说明），不混在色块里
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            selKey = "custom"
                            onSelect("custom", selHue)
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Box(
                        Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isCustom) preview else Color.hsl(selHue, 0.62f, 0.45f))
                            .border(
                                width = if (isCustom) 2.dp else 1.dp,
                                color = if (isCustom) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape,
                            ),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.accent_custom),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isCustom) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (isCustom) {
                    Spacer(Modifier.height(6.dp))
                    // 彩虹渐变轨道：颜色即色相预览；Slider 自身轨道透明只留 thumb。
                    // 定高 48dp：否则 Box wrap 到 4dp 轨道高，thumb 上下溢出压到文字
                    Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .drawBehind {
                                    drawRoundRect(
                                        brush = Brush.horizontalGradient(
                                            listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f)
                                                .map { Color.hsl(it, 0.62f, 0.45f) }
                                        ),
                                        cornerRadius = CornerRadius(size.height / 2f),
                                    )
                                },
                        )
                        Slider(
                            value = selHue,
                            onValueChange = { selHue = it },
                            onValueChangeFinished = { onSelect("custom", selHue) },
                            valueRange = 0f..360f,
                            modifier = Modifier.matchParentSize(),
                            thumb = { TriangleThumb() },
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.surface,
                                activeTrackColor = Color.Transparent,
                                inactiveTrackColor = Color.Transparent,
                            ),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
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

/** 双三角 thumb：上倒三角、下正三角夹住轨道，中间露出轨道颜色精确定位色相 */
@Composable
private fun TriangleThumb() {
    val fill = MaterialTheme.colorScheme.onSurface
    Box(
        Modifier
            .size(width = 20.dp, height = 32.dp)
            .drawBehind {
                val w = size.width
                val capH = w * 0.5f
                val path = Path()
                path.moveTo(0f, 0f)
                path.lineTo(w, 0f)
                path.lineTo(w / 2f, capH)
                path.close()
                path.moveTo(0f, size.height)
                path.lineTo(w, size.height)
                path.lineTo(w / 2f, size.height - capH)
                path.close()
                drawPath(path, color = fill)
            },
    )
}
