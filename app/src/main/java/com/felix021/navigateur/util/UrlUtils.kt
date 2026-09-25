package com.felix021.navigateur.util

import android.net.Uri
import android.util.Patterns
import android.webkit.URLUtil
import com.felix021.navigateur.data.SearchEngines

object UrlUtils {
    /** 内置起始页标记 URL */
    const val HOME = "about:home"

    fun isHome(url: String?): Boolean = url == HOME

    fun pretty(url: String): String =
        url.removePrefix("https://").removePrefix("http://").trimEnd('/')

    fun hostOf(url: String?): String = try {
        Uri.parse(url).host ?: ""
    } catch (e: Exception) {
        ""
    }

    /** 地址栏输入 → 实际加载的 URL；不像 URL 则按搜索词处理 */
    fun normalize(input: String, searchEngineId: String): String {
        val t = input.trim()
        if (t.isEmpty()) return ""
        if (URLUtil.isAboutUrl(t) || t.startsWith("data:")) return t
        val looksLikeUrl = Patterns.WEB_URL.matcher(t).matches() ||
            t.startsWith("localhost") ||
            t.startsWith("http://") ||
            t.startsWith("https://")
        if (looksLikeUrl && !t.contains(' ')) {
            return if (t.startsWith("http://") || t.startsWith("https://")) t else "https://$t"
        }
        return SearchEngines.urlFor(searchEngineId, t)
    }
}
