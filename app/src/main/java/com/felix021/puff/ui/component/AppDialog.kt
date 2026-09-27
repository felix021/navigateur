package com.felix021.puff.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/**
 * 全应用统一对话框壳：大圆角 + surfaceContainerHigh 底 + 统一标题色。
 * 各处只换函数名即可替换原生 AlertDialog；参数槽位与其一致。
 *
 * text 槽自动套滚动：内容最多占屏幕 55%（SS 出口表单这类长表单一屏放不下时
 * 可上下滚动），右缘给一条细滚动条提示下面还有内容；内容不满一屏时不显示。
 *
 * 注意：**text 槽内不要再套 verticalScroll**——嵌套滚动会让内层拿到无限高约束
 * 直接崩溃（IllegalStateException: infinity maximum height），滚动统一由外壳负责。
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
    val scrollable = scroll.maxValue > 0
    val barColor = MaterialTheme.colorScheme.onSurfaceVariant
    // 滚动时加深、静止时保持半显，让「还能滚」始终可感知
    val barAlpha by animateFloatAsState(
        targetValue = if (scroll.isScrollInProgress) 0.85f else 0.5f,
        animationSpec = tween(300),
        label = "dialogScrollbar",
    )

    Box(
        Modifier
            .fillMaxWidth()
            // 滚动条画在 Box 上（drawBehind），不加子节点：子节点会参与 Box 的
            // wrap 测量，曾把 text 槽撑满 AlertDialog 的 weight(1f) 份额，
            // 在内容与按钮之间留出半屏空洞（e70a463 改 matchParentSize 后
            // 子节点又拿不到尺寸、滚动条消失）。drawBehind 的 size 即视口，
            // 读滚动状态自动重绘，两头的坑都绕开。
            .drawBehind {
                if (!scrollable || size.height <= 0f) return@drawBehind
                val color = barColor
                val trackH = size.height
                val contentH = trackH + scroll.maxValue
                val thumbH = (trackH * trackH / contentH).coerceAtLeast(24f)
                val travel = (trackH - thumbH).coerceAtLeast(0f)
                val thumbTop = scroll.value / scroll.maxValue.toFloat() * travel
                val w = 4.dp.toPx()
                drawRoundRect(
                    color = color.copy(alpha = 0.15f * barAlpha),
                    topLeft = Offset(size.width - w, 0f),
                    size = Size(w, trackH),
                    cornerRadius = CornerRadius(w / 2f),
                )
                drawRoundRect(
                    color = color.copy(alpha = barAlpha),
                    topLeft = Offset(size.width - w, thumbTop),
                    size = Size(w, thumbH),
                    cornerRadius = CornerRadius(w / 2f),
                )
            },
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
    }
}
