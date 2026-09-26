package com.felix021.navigateur.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * 全应用统一对话框壳：大圆角 + surfaceContainerHigh 底 + 统一标题色。
 * 各处只换函数名即可替换原生 AlertDialog；参数槽位与其一致。
 *
 * text 槽自动套滚动：内容最多占屏幕 55%（SS 出口表单这类长表单一屏放不下时
 * 可上下滚动），右缘给一条细滚动条提示下面还有内容；内容不满一屏时不显示。
 */
@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier,
        dismissButton = dismissButton,
        title = title,
        text = text?.let { content -> { ScrollableDialogContent(content) } },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
    )
}

/**
 * 统一浮动菜单壳（搜索候选、底部菜单等）：与 AppDialog 同一套观感——
 * 16dp 圆角、surfaceContainerHigh 底、统一投影。菜单内容超高时系统自带滚动。
 */
@Composable
fun AppDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    androidx.compose.material3.DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
        content = content,
    )
}

/** text 槽：限高滚动 + 右缘细滚动条 */
@Composable
private fun ScrollableDialogContent(content: @Composable () -> Unit) {
    val scroll = rememberScrollState()
    val maxH = (LocalConfiguration.current.screenHeightDp * 0.55f).dp
    var viewportPx by remember { mutableIntStateOf(1) }
    val scrollable = scroll.maxValue > 0
    // 滚动时加深、静止时保持淡显，让「还能滚」始终可感知
    val barAlpha by animateFloatAsState(
        targetValue = if (scroll.isScrollInProgress) 0.85f else 0.35f,
        animationSpec = tween(300),
        label = "dialogScrollbar",
    )

    Box(
        Modifier
            .fillMaxWidth()
            .onSizeChanged { viewportPx = it.height },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = maxH)
                .verticalScroll(scroll)
                .padding(end = if (scrollable) 12.dp else 0.dp),
        ) {
            content()
        }
        if (scrollable) {
            DialogScrollBar(
                viewportPx = viewportPx,
                maxValue = scroll.maxValue,
                value = scroll.value,
                alpha = barAlpha,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(4.dp)
                    .fillMaxHeight(),
            )
        }
    }
}

/**
 * 竖向滚动条：轨道 = 容器高（≈视口），滑块按 滑块高=视口²/内容总高、
 * 位置=value/maxValue 换算，与 [androidx.compose.foundation.ScrollState] 同像素坐标系。
 */
@Composable
private fun DialogScrollBar(
    viewportPx: Int,
    maxValue: Int,
    value: Int,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier.alpha(alpha)) {
        val trackH = size.height
        if (trackH <= 0f || viewportPx <= 0) return@Canvas
        val contentH = viewportPx + maxValue
        val thumbH = (viewportPx / contentH.toFloat() * trackH).coerceAtLeast(24f)
        val travel = (trackH - thumbH).coerceAtLeast(0f)
        val thumbTop = if (maxValue > 0) value / maxValue.toFloat() * travel else 0f
        val w = size.width
        drawRoundRect(
            color = color.copy(alpha = 0.15f),
            topLeft = Offset(0f, 0f),
            size = Size(w, trackH),
            cornerRadius = CornerRadius(w / 2f),
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(0f, thumbTop),
            size = Size(w, thumbH),
            cornerRadius = CornerRadius(w / 2f),
        )
    }
}
