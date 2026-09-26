package com.felix021.navigateur.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.felix021.navigateur.R
import com.felix021.navigateur.ui.ACCENTS
import com.felix021.navigateur.ui.accentColorScheme
import com.felix021.navigateur.ui.component.AppDialog

/**
 * 主题色选择：六套预设 + 色相滑块自定义。
 * 滑块拖动只改本地预览，松手（onValueChangeFinished）才落盘，避免每帧 commit。
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

    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_accent)) },
        text = {
            Column {
                ACCENTS.forEach { opt ->
                    val selected = selKey == opt.key
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                            .clickable {
                                selKey = opt.key
                                selHue = opt.hue
                                onSelect(opt.key, opt.hue)
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(accentColorScheme(opt.key, opt.hue, dark).primary)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant,
                                    CircleShape,
                                ),
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            stringResource(opt.labelRes),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        if (selected) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .clickable {
                            selKey = "custom"
                            onSelect("custom", selHue)
                        }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.hsl(selHue, 0.62f, 0.45f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(
                        stringResource(R.string.accent_custom),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    if (isCustom) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                if (isCustom) {
                    Spacer(Modifier.height(4.dp))
                    Slider(
                        value = selHue,
                        onValueChange = { selHue = it },
                        onValueChangeFinished = { onSelect("custom", selHue) },
                        valueRange = 0f..360f,
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(preview)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                        )
                        Spacer(Modifier.width(10.dp))
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
