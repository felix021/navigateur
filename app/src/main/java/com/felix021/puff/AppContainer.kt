package com.felix021.puff

import android.content.Context
import com.felix021.puff.browser.AdBlockEngine
import com.felix021.puff.data.AdBlockRepository
import com.felix021.puff.data.BookmarkRepository
import com.felix021.puff.data.ProxyRepository
import com.felix021.puff.data.HistoryRepository
import com.felix021.puff.data.PasswordStore
import com.felix021.puff.data.SettingsRepository
import com.felix021.puff.data.TabSessionStore
import kotlinx.coroutines.launch

/** 持有全局单例，Application 创建时初始化 */
class AppContainer(context: Context) {
    val appContext = context.applicationContext
    val settings = SettingsRepository(appContext)
    val bookmarks = BookmarkRepository(appContext)
    val passwords = PasswordStore(appContext)
    val tabSessions = TabSessionStore(appContext)
    val history = HistoryRepository(appContext)
    val adBlockEngine = AdBlockEngine()
    val adBlock = AdBlockRepository(appContext, adBlockEngine).also { it.load() }
    val proxy = ProxyRepository()

    init {
        // 代理设置变化即生效（WebView 网络层覆盖，无需重载页面）
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            settings.settings.collect { proxy.apply(ProxyRepository.fromJson(it.proxyJson)) }
        }
    }
}
