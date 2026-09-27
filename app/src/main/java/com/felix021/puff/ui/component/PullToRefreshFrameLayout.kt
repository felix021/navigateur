package com.felix021.puff.ui.component

import android.animation.ValueAnimator
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.mutableStateOf
import kotlin.math.abs
import kotlin.math.exp

/**
 * WebView 容器 + 下拉刷新（SwipeRefreshLayout 思路的精简版）。
 *
 * 这个类直接充当 TabManager.webContainer（WebView 的直接父层）：Compose 的
 * AndroidView 会在 hit-test 后把触摸直喂目标 View，**父容器的
 * onInterceptTouchEvent 不在路径上**，所以手势挂在每个子 View 的
 * OnTouchListener 上转发到这里统一处理：
 * - 只有页面在顶部（子 View canScrollVertically(-1) == false）且向下拖过
 *   touchSlop 才接管，接管瞬间给 WebView 发 CANCEL，避免它继续跟手/闪 glow
 * - 阻尼量做指数衰减（越拉越紧），松手超过阈值触发 [onRefresh] 并停在停留
 *   高度，未达标弹回 0；刷新完成由 Compose 层调 [finishRefresh] 收起
 *
 * **内容不跟手**（M3 / Chrome / SwipeRefreshLayout 同款交互）：只有指示器
 * 跟手。曾试过每帧改 WebView translationY 让页面跟着下移，结果 Chromium
 * 每帧都要重新同步表面，真机上表现为页面疯狂闪烁（反复重绘），故废弃。
 *
 * 指示器动效不在本类画：dragOffsetPx / refreshing 以 Compose State 暴露，
 * 由 BrowserContent 的指示器 overlay 跟随绘制（主题色 / 形状与全应用一致）。
 */
class PullToRefreshFrameLayout @JvmOverloads constructor(
    context: Context,
) : FrameLayout(context) {

    var onRefresh: (() -> Unit)? = null

    /** 当前内容下移像素（含弹回动画中间值），指示器按它跟手 */
    val dragOffsetPx = mutableStateOf(0f)

    /** 已触发刷新（等待页面 loading 结束调 finishRefresh） */
    val refreshing = mutableStateOf(false)

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private val density = resources.displayMetrics.density
    private val triggerPx = TRIGGER_DP * density
    private val maxDragPx = MAX_DRAG_DP * density
    private val restPx = REST_DP * density

    private var downY = 0f
    private var lastY = 0f
    private var rawDrag = 0f
    private var dragging = false
    /** 接管瞬间给 WebView 发 CANCEL 会再次进本类 listener，用它跳过 */
    private var sendingCancel = false
    private var anim: ValueAnimator? = null

    /** 页面加载结束：收起指示器与位移 */
    fun finishRefresh() {
        refreshing.value = false
        animateTo(0f)
    }

    /** 子 View（WebView）挂进容器时统一接上下拉手势 */
    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        super.addView(child, index, params)
        child.setOnTouchListener(::onChildTouch)
    }

    /**
     * WebView 的触摸转发。返回 false 时 WebView 正常处理；下拉接管后返回 true
     * 全部消费。注意这里只在 View 自身分发内拦（listener 早于 onTouchEvent），
     * 不依赖 ViewGroup 的 intercept 机制。
     */
    private fun onChildTouch(v: View, ev: MotionEvent): Boolean {
        if (sendingCancel) return false
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                anim?.cancel()   // 弹回动画途中再拉：立刻止损，别让动画和拖拽抢位移
                downY = ev.y
                lastY = ev.y
                rawDrag = 0f
                dragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragging) {
                    rawDrag += ev.y - lastY
                    lastY = ev.y
                    applyOffset(damped(rawDrag))
                    return true
                }
                if (!refreshing.value) {
                    val dy = ev.y - downY
                    if (dy > touchSlop && !v.canScrollVertically(-1)) {
                        dragging = true
                        lastY = ev.y
                        // WebView 已开始跟随手指，接管前先发 CANCEL 清掉它的状态
                        val cancel = MotionEvent.obtain(ev).apply {
                            action = MotionEvent.ACTION_CANCEL
                        }
                        sendingCancel = true
                        v.dispatchTouchEvent(cancel)
                        sendingCancel = false
                        cancel.recycle()
                        return true
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> if (dragging) {
                dragging = false
                if (dragOffsetPx.value >= triggerPx && !refreshing.value) {
                    refreshing.value = true
                    onRefresh?.invoke()
                    animateTo(restPx)
                } else {
                    animateTo(0f)
                }
                return true
            }
        }
        return false
    }

    /** 指数阻尼：raw 越大每像素「换到」的位移越小，拉到头也拉不断 */
    private fun damped(raw: Float): Float =
        maxDragPx * (1f - exp(-raw / (maxDragPx * 0.75f)))

    private fun applyOffset(px: Float) {
        // 只写状态供指示器跟手；内容（WebView）保持不动，原因见类注释
        dragOffsetPx.value = px
    }

    private fun animateTo(target: Float) {
        anim?.cancel()
        val from = dragOffsetPx.value
        if (abs(target - from) < 1f) {
            applyOffset(target)
            return
        }
        anim = ValueAnimator.ofFloat(from, target).apply {
            duration = if (target > from) 220L else 280L
            interpolator = android.view.animation.DecelerateInterpolator(2f)
            addUpdateListener {
                applyOffset(it.animatedValue as Float)
            }
            start()
        }
    }

    companion object {
        /** 松手触发刷新的下拉阈值 */
        const val TRIGGER_DP = 64f
        /** 阻尼上限对应的位移 */
        const val MAX_DRAG_DP = 140f
        /** 刷新中指示器的停留位移（悬在页面顶部转圈） */
        const val REST_DP = 56f
    }
}
