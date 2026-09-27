package com.felix021.puff.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

/**
 * 双三角滑块 thumb：上倒三角、下正三角夹住轨道，中间 12dp 透明带露出轨道——
 * 尖端即当前值，颜色可直接读轨道。需放在高 ≥40dp 的滑动区域内（thumb 垂直居中）。
 */
@Composable
fun TriangleThumb(color: Color) {
    Box(
        Modifier
            .size(width = 28.dp, height = 40.dp)
            .drawBehind {
                val w = size.width
                val capH = w * 0.46f
                val r = w * 0.22f
                drawPath(
                    roundedTriangle(
                        listOf(Offset(0f, 0f), Offset(w, 0f), Offset(w / 2f, capH)), r,
                    ), color,
                )
                drawPath(
                    roundedTriangle(
                        listOf(
                            Offset(0f, size.height), Offset(w, size.height),
                            Offset(w / 2f, size.height - capH),
                        ), r,
                    ), color,
                )
            },
    )
}

/** 顶点圆角化的三角形路径：每个顶点用二次贝塞尔切出圆弧 */
private fun roundedTriangle(pts: List<Offset>, r: Float): Path {
    val path = Path()
    val n = pts.size
    for (i in 0 until n) {
        val prev = pts[(i + n - 1) % n]
        val cur = pts[i]
        val next = pts[(i + 1) % n]
        val in1 = (prev - cur)
        val in2 = (next - cur)
        val p1 = cur + in1 * (r / in1.getDistance())
        val p2 = cur + in2 * (r / in2.getDistance())
        if (i == 0) path.moveTo(p1.x, p1.y) else path.lineTo(p1.x, p1.y)
        path.quadraticBezierTo(cur.x, cur.y, p2.x, p2.y)
    }
    path.close()
    return path
}
