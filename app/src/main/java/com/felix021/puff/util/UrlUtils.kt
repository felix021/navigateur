package com.felix021.puff.util

import android.net.Uri
import android.util.Patterns
import android.webkit.URLUtil
import com.felix021.puff.data.SearchEngines

object UrlUtils {
    /** 内置起始页标记 URL */
    const val HOME = "about:home"

    /** 常见下载文件扩展名（与页面层 DOWNLOAD_INTERCEPT 保持一致） */
    private val DOWNLOAD_EXT = Regex(
        "\\.(zip|rar|7z|apk|exe|dmg|msi|pdf|tar|gz|tgz|bz2|xz|iso|mp3|flac|wav|mp4|avi|mkv|mov|epub|mobi|torrent|doc|docx|xls|xlsx|ppt|pptx|csv)$",
        RegexOption.IGNORE_CASE,
    )

    fun isDownloadLike(url: String): Boolean = try {
        DOWNLOAD_EXT.containsMatchIn(Uri.parse(url).path.orEmpty())
    } catch (e: Exception) {
        false
    }

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
