package com.felix021.navigateur

import android.content.Context
import com.felix021.navigateur.browser.AdBlockEngine
import com.felix021.navigateur.data.AdBlockRepository
import com.felix021.navigateur.data.BookmarkRepository
import com.felix021.navigateur.data.HistoryRepository
import com.felix021.navigateur.data.PasswordStore
import com.felix021.navigateur.data.SettingsRepository
import com.felix021.navigateur.data.TabSessionStore

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
}
