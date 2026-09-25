package cn.felix021.navigateur.browser

import android.content.Intent
import android.graphics.Bitmap
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import cn.felix021.navigateur.util.UrlUtils

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

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        if (skip(url)) return
        tm.updateState(tabId) { it.copy(url = url, loading = true, progress = 0, error = null) }
        // 捕获 hook 尽早注入，不依赖 onPageFinished（幂等）
        tm.injectCaptureHook(view)
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
        tm.refreshNav()
        tm.persist()
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
        if (url == null || skip(url) || UrlUtils.isHome(url)) return
        // 每次导航必然回调：在此兜底注入捕获 hook（幂等，SPA 场景也覆盖）
        tm.injectCaptureHook(view)
        tm.updateState(tabId) { if (it.url != url) it.copy(url = url) else it }
        tm.refreshNav()
        tm.persist()
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError,
    ) {
        if (!request.isForMainFrame) return
        val desc = error.description?.toString().orEmpty().ifEmpty { "网络错误" }
        tm.updateState(tabId) { it.copy(loading = false, error = desc) }
    }

    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        tm.onRenderGone(tabId)
        return true
    }

    private fun skip(url: String?) = url == "about:blank"
}
