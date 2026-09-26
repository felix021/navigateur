package com.felix021.puff.browser

import android.net.Uri
import android.util.Log
import org.json.JSONObject
import android.webkit.JavascriptInterface

/**
 * 页面 JS → Kotlin 的唯一桥。
 * 安全约束：只暴露一个只收不发的方法，调用时校验所属标签的当前 URL。
 */
class NativeBridge(
    private val tabId: String,
    private val tabManager: TabManager,
) {
    @JavascriptInterface
    fun onCredentials(json: String) {
        try {
            val o = JSONObject(json)
            val user = o.optString("username")
            val pass = o.optString("password")
            if (pass.isEmpty() || user.length > 320 || pass.length > 256) return
            val url = tabManager.urlOf(tabId) ?: return
            if (!url.startsWith("https://")) return
            val host = Uri.parse(url).host ?: return
            if (host.isEmpty()) return
            tabManager.postCredentials(host, user, pass)
        } catch (e: Exception) {
            Log.w("Puff", "bridge error", e)
        }
    }

    /** 页面层拦截到的下载链接（新版 WebView 不再回调 onDownloadStart 的兜底主路径） */
    @JavascriptInterface
    fun download(url: String) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) return
        tabManager.postDownload(url)
    }
}
