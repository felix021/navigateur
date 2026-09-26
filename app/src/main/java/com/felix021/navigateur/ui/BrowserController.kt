package com.felix021.navigateur.ui

import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebViewDatabase
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import com.felix021.navigateur.MainActivity
import com.felix021.navigateur.NavigateurApp
import com.felix021.navigateur.browser.TabManager
import com.felix021.navigateur.util.UrlUtils
import com.felix021.navigateur.R

enum class Screen { Browser, Tabs, Bookmarks, Settings }

/** Activity 级控制器：聚合 TabManager 与页面导航状态 */
class BrowserController(val activity: MainActivity) {
    val container = (activity.application as NavigateurApp).container
    val tabManager = TabManager(activity, container, ::handleCredentials)
    val screen = mutableStateOf(Screen.Browser)

    data class SavePrompt(
        val host: String,
        val username: String,
        val password: String,
        /** 非空表示站点已有其他账号，保存将替换它 */
        val replacing: String?,
    )

    val savePrompt = mutableStateOf<SavePrompt?>(null)

    /** 浏览页按返回且无页面历史时，先询问再退出 */
    val exitConfirm = mutableStateOf(false)

    /** 页面边缘平均色（ARGB），供自绘状态条配色；null = 未取到用默认深色 */
    val edgeColor = mutableStateOf<Int?>(null)

    /** 用 PixelCopy 抓窗口在 stripLeftPx 处的竖条平均色（页面左缘） */
    fun refreshEdgeColor(stripLeftPx: Int) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) return
        val decor = activity.window.decorView // 仅用于取宽高
        val h = decor.height
        val w = 12
        val x = stripLeftPx.coerceIn(0, (decor.width - w).coerceAtLeast(0))
        if (h <= 0 || decor.width < w) return
        val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        try {
            val listener = android.view.PixelCopy.OnPixelCopyFinishedListener { result ->
                if (result == android.view.PixelCopy.SUCCESS) {
                    var r = 0L; var g = 0L; var b = 0L; var n = 0L
                    for (y in 0 until h step 24) {
                        for (xx in 0 until w step 6) {
                            val c = bmp.getPixel(xx, y)
                            r += (c shr 16) and 0xFF
                            g += (c shr 8) and 0xFF
                            b += c and 0xFF
                            n++
                        }
                    }
                    if (n > 0) {
                        edgeColor.value =
                            (0xFF shl 24) or ((r / n).toInt() shl 16) or ((g / n).toInt() shl 8) or (b / n).toInt()
                    }
                }
                bmp.recycle()
            }
            android.view.PixelCopy.request(
                activity.window,
                android.graphics.Rect(x, 0, x + w, h),
                bmp,
                listener,
                android.os.Handler(android.os.Looper.getMainLooper()),
            )
        } catch (e: Exception) {
            bmp.recycle()
        }
    }

    fun exitApp() {
        activity.finish()
    }

    fun handleCredentials(host: String, user: String, pass: String) {
        if (!container.settings.current.savePasswords) return
        val existing = container.passwords.get(host)
        if (existing != null && existing.username == user && existing.password == pass) return
        savePrompt.value = SavePrompt(host, user, pass, existing?.username?.takeIf { it != user })
    }

    fun confirmSavePrompt() {
        savePrompt.value?.let { container.passwords.upsert(it.host, it.username, it.password) }
        savePrompt.value = null
    }

    fun dismissSavePrompt() {
        savePrompt.value = null
    }

    fun loadOrSearch(input: String) {
        val url = UrlUtils.normalize(input, container.settings.current.searchEngineId)
        if (url.isNotEmpty()) {
            // 地址栏直输下载文件 URL：直接走系统下载（新版 WebView 导航下载不可靠）
            if (UrlUtils.isDownloadLike(url)) tabManager.postDownload(url)
            else tabManager.loadInCurrent(url)
        }
        screen.value = Screen.Browser
    }

    fun openInCurrent(url: String) {
        tabManager.loadInCurrent(url)
        screen.value = Screen.Browser
    }

    fun toggleBookmark() {
        val tab = tabManager.current ?: return
        if (UrlUtils.isHome(tab.url)) return
        if (container.bookmarks.isBookmarked(tab.url)) {
            container.bookmarks.removeByUrl(tab.url)
        } else {
            container.bookmarks.add(tab.title, tab.url)
        }
    }

    fun clearData(
        cookies: Boolean,
        siteStorage: Boolean,
        cache: Boolean,
        formData: Boolean,
        passwords: Boolean,
        history: Boolean,
    ) {
        if (cookies) CookieManager.getInstance().removeAllCookies(null)
        if (siteStorage) WebStorage.getInstance().deleteAllData()
        if (cache) tabManager.webViews.values.firstOrNull()?.clearCache(true)
        if (formData) {
            runCatching { WebViewDatabase.getInstance(activity).clearFormData() }
            runCatching { WebViewDatabase.getInstance(activity).clearHttpAuthUsernamePassword() }
        }
        if (passwords) container.passwords.clear()
        if (history) container.history.clear()
        Toast.makeText(activity, activity.getString(R.string.cleared), Toast.LENGTH_SHORT).show()
    }

    /** 已知站点列表（密码库 + 书签 + 历史），用于按站点清理 */
    fun knownHosts(): List<String> = buildSet {
        container.passwords.hosts().forEach(::add)
        container.bookmarks.bookmarks.value.forEach { UrlUtils.hostOf(it.url).takeIf { h -> h.isNotBlank() }?.let(::add) }
        container.history.hosts().forEach(::add)
    }.sorted()

    /**
     * 按站点清理。cookie 无公开按域删除 API，用逐个过期的方式尽力清理；
     * 站点存储用 WebStorage.deleteOrigin（localStorage/WebSQL）。
     */
    fun clearSiteData(
        host: String,
        cookies: Boolean,
        siteStorage: Boolean,
        passwords: Boolean,
        history: Boolean,
    ) {
        if (cookies) {
            val cm = CookieManager.getInstance()
            listOf("https://$host/", "http://$host/").forEach { url ->
                runCatching {
                    cm.getCookie(url)?.split(";")?.forEach { pair ->
                        val name = pair.substringBefore('=').trim()
                        if (name.isNotEmpty()) {
                            cm.setCookie(url, "$name=; Path=/; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT")
                        }
                    }
                }
            }
            cm.flush()
        }
        if (siteStorage) {
            runCatching { WebStorage.getInstance().deleteOrigin(host) }
        }
        if (passwords) container.passwords.remove(host)
        if (history) container.history.removeByHost(host)
        Toast.makeText(activity, activity.getString(R.string.cleared_site, host), Toast.LENGTH_SHORT).show()
    }
}
