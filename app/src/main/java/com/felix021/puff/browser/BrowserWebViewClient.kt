package com.felix021.puff.browser

import android.content.Intent
import android.graphics.Bitmap
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.felix021.puff.util.UrlUtils
import com.felix021.puff.R

class BrowserWebViewClient(
    private val tm: TabManager,
    private val tabId: String,
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val scheme = request.url.scheme?.lowercase()
        return when (scheme) {
            "http", "https", "about", "data", "blob", "javascript" -> false
            else -> {
                // intent://、market:// 等交给系统
                try {
                    tm.context.startActivity(Intent(Intent.ACTION_VIEW, request.url))
                } catch (e: Exception) {
                }
                true
            }
        }
    }

    /** 广告拦截：子资源请求命中规则时返回空响应（主文档永不拦）。
     *  注意本回调在后台线程，不能触碰 WebView 实例方法，页面 host 走 TabManager 的缓存 */
    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest,
    ): WebResourceResponse? {
        val s = tm.container.settings.current
        if (request.isForMainFrame) {
            // 主文档请求必然先于其子资源到达这里：就地刷新 host 缓存，
            // 消除 onPageStarted 回调前的时序窗口（否则白名单站点首屏仍会被拦）
            tm.refreshCurrentHost(tabId, request.url.toString())
            return null
        }
        if (!s.adBlockEnabled) return null
        val pageHost = tm.currentHost
        if (pageHost.isNotEmpty() && s.adBlockAllowlist.any { pageHost == it || pageHost.endsWith(".$it") }) {
            return null
        }
        if (tm.container.adBlockEngine.shouldBlock(request.url.toString())) {
            return WebResourceResponse("text/plain", "utf-8", java.io.ByteArrayInputStream(ByteArray(0)))
        }
        return null
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        if (skip(url)) return
        tm.updateState(tabId) { it.copy(url = url, loading = true, progress = 0, error = null) }
        tm.refreshCurrentHost(tabId, url)
        // 捕获 hook 尽早注入，不依赖 onPageFinished（幂等）
        tm.injectCaptureHook(view)
        view.evaluateJavascript(JsScripts.DOWNLOAD_INTERCEPT, null)
        tm.refreshNav()
        tm.persist()
    }

    override fun onPageCommitVisible(view: WebView, url: String) {
        if (skip(url) || UrlUtils.isHome(url)) return
        // onload 挂起的页面也尽量完成字体/自动填充注入
        tm.injectForPage(tabId, view, url)
    }

    override fun onPageFinished(view: WebView, url: String) {
        if (skip(url)) return
        tm.updateState(tabId) { it.copy(loading = false, progress = 100) }
        tm.injectForPage(tabId, view, url)
        tm.recordHistory(tabId, url)
        tm.refreshNav()
        tm.persist()
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
        if (url == null || skip(url) || UrlUtils.isHome(url)) return
        // 每次导航必然回调：在此兜底注入捕获 hook（幂等，SPA 场景也覆盖）
        tm.injectCaptureHook(view)
        view.evaluateJavascript(JsScripts.DOWNLOAD_INTERCEPT, null)
        tm.recordHistory(tabId, url)
        tm.updateState(tabId) { if (it.url != url) it.copy(url = url) else it }
        tm.refreshCurrentHost(tabId, url)
        tm.refreshNav()
        tm.persist()
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError,
    ) {
        if (!request.isForMainFrame) return
        val desc = error.description?.toString().orEmpty()
            .ifEmpty { tm.context.getString(R.string.network_error) }
        tm.updateState(tabId) { it.copy(loading = false, error = desc) }
    }

    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        tm.onRenderGone(tabId)
        return true
    }

    private fun skip(url: String?) = url == "about:blank"
}
