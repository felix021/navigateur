package cn.felix021.navigateur.browser

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
            Log.w("Navigateur", "bridge error", e)
        }
    }
}
