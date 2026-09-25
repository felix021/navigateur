package cn.felix021.navigateur.ui

import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebViewDatabase
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import cn.felix021.navigateur.MainActivity
import cn.felix021.navigateur.NavigateurApp
import cn.felix021.navigateur.browser.TabManager
import cn.felix021.navigateur.util.UrlUtils

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
        if (url.isNotEmpty()) tabManager.loadInCurrent(url)
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
        Toast.makeText(activity, "已清理", Toast.LENGTH_SHORT).show()
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
        Toast.makeText(activity, "已清理 $host", Toast.LENGTH_SHORT).show()
    }
}
