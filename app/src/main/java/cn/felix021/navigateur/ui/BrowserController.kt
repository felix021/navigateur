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
    ) {
        if (cookies) CookieManager.getInstance().removeAllCookies(null)
        if (siteStorage) WebStorage.getInstance().deleteAllData()
        if (cache) tabManager.webViews.values.firstOrNull()?.clearCache(true)
        if (formData) {
            runCatching { WebViewDatabase.getInstance(activity).clearFormData() }
            runCatching { WebViewDatabase.getInstance(activity).clearHttpAuthUsernamePassword() }
        }
        if (passwords) container.passwords.clear()
        Toast.makeText(activity, "已清理", Toast.LENGTH_SHORT).show()
    }
}
