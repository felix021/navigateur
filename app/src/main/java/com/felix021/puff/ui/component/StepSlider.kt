@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.felix021.puff.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.felix021.puff.R
import kotlin.math.roundToInt

/**
 * 带两端 +/- 步进键的离散滑块：拖动吸附到 [step] 档位，按钮每次 ±[step]，
 * 到边界时对应按钮自动禁用。值始终落在 [range] 内且为 [step] 的倍数偏移。
 *
 * 轨道与 thumb 自绘（双三角夹轨道）：M3 默认 thumb 两侧有白色 track gap，
 * 且竖条 thumb 太细不易点按，故 track 画透明、由 Canvas 按 value 画双色轨道。
 */
@Composable
fun StepSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    range: IntRange = 50..200,
    step: Int = 5,
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.surfaceVariant
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            enabled = value - step >= range.first,
            onClick = { onValueChange((value - step).coerceIn(range.first, range.last)) },
        ) {
            Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.zoom_decrease))
        }
        Box(Modifier.weight(1f).height(48.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxWidth().height(4.dp)) {
                val fraction = ((value - range.first).toFloat() / (range.last - range.first))
                    .coerceIn(0f, 1f)
                val activeW = size.width * fraction
                val r = CornerRadius(size.height / 2f)
                drawRoundRect(inactiveColor, Offset(activeW, 0f), Size(size.width - activeW, size.height), r)
                drawRoundRect(activeColor, Offset(0f, 0f), Size(activeW, size.height), r)
            }
            androidx.compose.material3.Slider(
                value = value.toFloat(),
                onValueChange = {
                    onValueChange(((it / step).roundToInt() * step).coerceIn(range.first, range.last))
                },
                valueRange = range.first.toFloat()..range.last.toFloat(),
                modifier = Modifier.matchParentSize(),
                thumb = { TriangleThumb(activeColor) },
                colors = SliderDefaults.colors(
                    activeTrackColor = androidx.compose.ui.graphics.Color.Transparent,
                    inactiveTrackColor = androidx.compose.ui.graphics.Color.Transparent,
                ),
            )
        }
        IconButton(
            enabled = value + step <= range.last,
            onClick = { onValueChange((value + step).coerceIn(range.first, range.last)) },
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.zoom_increase))
        }
    }
}
