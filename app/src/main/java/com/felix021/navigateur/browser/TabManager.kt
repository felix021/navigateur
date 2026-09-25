package com.felix021.navigateur.browser

import android.content.Context
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.compose.runtime.mutableStateOf
import com.felix021.navigateur.AppContainer
import com.felix021.navigateur.data.BrowserSettings
import com.felix021.navigateur.data.ThemeMode
import com.felix021.navigateur.util.Downloader
import com.felix021.navigateur.util.UrlUtils
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 多标签管理：每个标签一个 WebView 实例，仅当前标签 attach 到视图树。
 * WebView 必须用 Activity context 创建（下拉/弹窗依赖）。
 */
class TabManager(
    val context: Context,
    internal val container: AppContainer,
    private val onCredentials: (host: String, user: String, pass: String) -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _tabs = MutableStateFlow<List<TabState>>(emptyList())
    val tabs: StateFlow<List<TabState>> = _tabs
    private val _currentId = MutableStateFlow<String?>(null)
    val currentId: StateFlow<String?> = _currentId

    /** 当前标签导航状态 (canGoBack, canGoForward) */
    val nav = mutableStateOf(Pair(false, false))

    val webViews = HashMap<String, WebView>()
    var webContainer: FrameLayout? = null

    private var lastAppliedUa: String? = null
    private var lastAppliedDark: Boolean? = null
    private var lastAppliedZoom: Int? = null
    private var lastAppliedAd: String? = null
    private var lastAppliedDev: Boolean? = null
    private var lastAppliedRemote: Boolean? = null
    private var erudaJs: String? = null

    /** 页面内开发者工具：注入（幂等）/ 移除 eruda */
    private fun injectDevTools(wv: WebView, enabled: Boolean) {
        if (enabled) {
            val src = erudaJs ?: runCatching {
                context.assets.open("devtools/eruda.min.js").bufferedReader().readText()
            }.getOrNull()?.also { erudaJs = it } ?: return
            wv.evaluateJavascript(
                // 换行必不可少：eruda.min.js 以 //# sourceMappingURL 行注释结尾，
                // 不换行会把后面的 init 调用一起注释掉
                "(function(){if(window.__nvEruda)return;\n" + src + "\n;eruda.init();window.__nvEruda=1;})();",
                null,
            )
        } else {
            wv.evaluateJavascript(
                "(function(){try{if(window.__nvEruda){window.eruda.destroy();window.__nvEruda=0}}catch(e){}})();",
                null,
            )
        }
    }

    /** 当前标签页面 host 的缓存（主线程写、请求线程读，供 shouldInterceptRequest 白名单判断） */
    @Volatile
    var currentHost: String = ""
        private set

    internal fun refreshCurrentHost(tabId: String, url: String?) {
        if (tabId != _currentId.value || url.isNullOrEmpty()) return
        val h = UrlUtils.hostOf(url)
        if (UrlUtils.isHome(url)) {
            if (currentHost.isNotEmpty()) currentHost = ""
        } else if (h != currentHost) {
            currentHost = h
        }
    }

    /** 广告拦截生效状态的指纹（开关 + 白名单） */
    private fun BrowserSettings.adBlockKey(): String =
        (if (adBlockEnabled) "on|" else "off|") + adBlockAllowlist.sorted().joinToString(",")

    val current: TabState? get() = _tabs.value.firstOrNull { it.id == _currentId.value }
    val currentWebView: WebView? get() = _currentId.value?.let { webViews[it] }

    init {
        restore()
        scope.launch { container.settings.settings.collect { applySettings(it) } }
    }

    // ---------- 标签生命周期 ----------

    fun newTab(url: String = container.settings.current.homepage): String {
        val id = UUID.randomUUID().toString()
        val state = TabState(
            id = id,
            url = url,
            title = if (UrlUtils.isHome(url)) "起始页" else "",
            desktopMode = container.settings.current.desktopModeDefault,
        )
        _tabs.value = _tabs.value + state
        _currentId.value = id
        if (!UrlUtils.isHome(url)) webViewFor(id)
        syncWebView()
        persist()
        return id
    }

    fun switchTo(id: String) {
        if (_tabs.value.none { it.id == id } || id == _currentId.value) return
        _currentId.value = id
        syncWebView()
        persist()
    }

    fun closeTab(id: String) {
        val idx = _tabs.value.indexOfFirst { it.id == id }
        if (idx < 0) return
        val wasCurrent = id == _currentId.value
        _tabs.value = _tabs.value.filterNot { it.id == id }
        webViews.remove(id)?.let(::destroyWebView)
        when {
            _tabs.value.isEmpty() -> newTab()
            wasCurrent -> {
                val next = _tabs.value[minOf(idx, _tabs.value.size - 1)]
                _currentId.value = next.id
                syncWebView()
            }
        }
        refreshNav()
        persist()
    }

    fun closeAll() {
        webViews.values.forEach(::destroyWebView)
        webViews.clear()
        _tabs.value = emptyList()
        newTab()
    }

    fun destroyAll() {
        webViews.values.forEach(::destroyWebView)
        webViews.clear()
    }

    /** 按需创建 WebView（重启恢复的标签首次切换到时才真正创建并加载） */
    fun webViewFor(tabId: String): WebView? {
        webViews[tabId]?.let { return it }
        val state = _tabs.value.firstOrNull { it.id == tabId } ?: return null
        val wv = createWebView(state)
        if (!UrlUtils.isHome(state.url) && state.url.isNotBlank()) wv.loadUrl(state.url)
        return wv
    }

    private fun createWebView(state: TabState): WebView {
        val wv = WebView(context)
        wv.tag = state.id
        WebViewFactory.configure(
            context, wv,
            container.settings.current, state.desktopMode,
            effectiveDark(container.settings.current),
        )
        wv.webViewClient = BrowserWebViewClient(this, state.id)
        wv.webChromeClient = BrowserChromeClient(this, state.id)
        wv.addJavascriptInterface(NativeBridge(state.id, this), "NavigateurBridge")
        wv.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            Downloader.start(
                context, url, userAgent, contentDisposition, mimeType,
            )
        }
        webViews[state.id] = wv
        return wv
    }

    private fun destroyWebView(wv: WebView) {
        (wv.parent as? ViewGroup)?.removeView(wv)
        runCatching {
            wv.removeAllViews()
            wv.destroy()
        }
    }

    // ---------- 视图同步 ----------

    /** 把当前标签的 WebView attach 到容器，其余 detach（BrowserScreen 的 AndroidView 调用） */
    fun syncWebView() {
        val c = webContainer ?: return
        val id = _currentId.value ?: return
        // 起始页不挂 WebView：空白 WebView 会抢占触摸/焦点，Compose 起始页无法交互
        if (UrlUtils.isHome(current?.url)) {
            for (i in c.childCount - 1 downTo 0) {
                val child = c.getChildAt(i)
                c.removeView(child)
                (child as? WebView)?.onPause()
            }
            refreshNav()
            return
        }
        val wv = webViewFor(id) ?: return
        for (i in c.childCount - 1 downTo 0) {
            val child = c.getChildAt(i)
            if (child !== wv) {
                c.removeView(child)
                (child as? WebView)?.onPause()
            }
        }
        // 注意：横竖屏切换会重建容器（AndroidView factory），WebView 可能还挂在旧容器上，
        // 必须比较 parent 是否为当前容器，否则页面白屏
        val parent = wv.parent
        if (parent !== c) {
            (parent as? android.view.ViewGroup)?.removeView(wv)
            c.addView(wv, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ))
        }
        wv.onResume()
        refreshNav()
    }

    fun refreshNav() {
        val wv = currentWebView
        nav.value = (wv?.canGoBack() == true) to (wv?.canGoForward() == true)
        current?.let { refreshCurrentHost(it.id, it.url) }
    }

    // ---------- 导航 ----------

    fun loadInCurrent(url: String) {
        if (url.isEmpty()) return
        if (UrlUtils.isHome(url)) {
            showHome()
            return
        }
        val id = _currentId.value
        if (id == null) {
            newTab(url)
            return
        }
        updateState(id) { it.copy(url = url, title = "", loading = true, progress = 0, error = null) }
        val wv = webViews[id]
        if (wv != null) wv.loadUrl(url) else webViewFor(id) // 新建的会按 state.url 加载
        syncWebView()
        refreshNav()
        persist()
    }

    /** 回到主页：内置起始页则重置当前标签；自定义 URL 则加载 */
    fun goHome() {
        val home = container.settings.current.homepage
        if (UrlUtils.isHome(home)) showHome() else loadInCurrent(home)
    }

    private fun showHome() {
        val id = _currentId.value ?: run { newTab(); return }
        val wv = webViews[id]
        wv?.stopLoading()
        wv?.loadUrl("about:blank")
        wv?.clearHistory()
        updateState(id) {
            it.copy(url = UrlUtils.HOME, title = "起始页", loading = false, progress = 100, error = null)
        }
        syncWebView()
        refreshNav()
        persist()
    }

    fun goBack() { currentWebView?.let { if (it.canGoBack()) it.goBack() } }
    fun goForward() { currentWebView?.let { if (it.canGoForward()) it.goForward() } }
    fun reloadCurrent() { currentWebView?.reload() }

    fun setDesktopMode(tabId: String, enabled: Boolean) {
        val tab = _tabs.value.firstOrNull { it.id == tabId } ?: return
        if (tab.desktopMode == enabled) return
        updateState(tabId) { it.copy(desktopMode = enabled) }
        val wv = webViews[tabId] ?: return
        WebViewFactory.applyUserAgent(context, wv, container.settings.current, enabled)
        if (!UrlUtils.isHome(tab.url)) wv.reload()
        persist()
    }

    // ---------- 供 client / bridge 调用 ----------

    fun updateState(tabId: String, transform: (TabState) -> TabState) {
        _tabs.value = _tabs.value.map { if (it.id == tabId) transform(it) else it }
    }

    fun urlOf(tabId: String): String? =
        _tabs.value.firstOrNull { it.id == tabId }?.url

    fun postCredentials(host: String, user: String, pass: String) {
        handler.post { onCredentials(host, user, pass) }
    }

    /** JS 桥下载：走系统 DownloadManager（Cookie/UA 在 Downloader 内处理） */
    fun postDownload(url: String) {
        handler.post {
            val wv = currentWebView ?: return@post
            Downloader.start(context, url, wv.settings.userAgentString, null, null)
        }
    }

    fun injectForPage(tabId: String, wv: WebView, url: String) {
        val s = container.settings.current
        wv.evaluateJavascript(JsScripts.fontCss(s.fontFamily), null)
        wv.evaluateJavascript(JsScripts.zoomCss(s.pageZoomPercent), null)
        // 广告元素隐藏（请求拦截在 WebViewClient.shouldInterceptRequest）
        injectAdHide(wv, url)
        if (s.devTools) injectDevTools(wv, true)
        // 桌面模式标签：覆盖 viewport 为宽屏，让响应式站点出桌面布局
        if (tabOf(wv)?.desktopMode == true) {
            wv.evaluateJavascript(JsScripts.DESKTOP_VIEWPORT, null)
        }
        injectCaptureHook(wv)
        wv.evaluateJavascript(JsScripts.DOWNLOAD_INTERCEPT, null)
        if (url.startsWith("https://")) {
            val host = UrlUtils.hostOf(url)
            if (host.isNotEmpty()) {
                container.passwords.get(host)?.let {
                    wv.evaluateJavascript(JsScripts.autofill(it.username, it.password), null)
                }
            }
        }
    }

    /** 捕获 hook 尽早注入（onPageStarted/doUpdateVisitedHistory）：有些站点 onload 挂起导致 onPageFinished 迟迟不来 */
    fun injectCaptureHook(wv: WebView) {
        if (!container.settings.current.savePasswords) return
        wv.evaluateJavascript(JsScripts.CAPTURE_HOOK, null)
    }

    /** 广告元素隐藏 CSS：替换 id=nv-adhide 的样式元素，空 CSS 即移除（关白名单场景） */
    private fun injectAdHide(wv: WebView, url: String) {
        val s = container.settings.current
        val host = UrlUtils.hostOf(url)
        val css = if (s.adBlockEnabled && host.isNotEmpty() &&
            s.adBlockAllowlist.none { host == it || host.endsWith(".$it") }
        ) {
            container.adBlockEngine.hideCss(host)
        } else ""
        wv.evaluateJavascript(JsScripts.styleCss("nv-adhide", css), null)
    }

    /** 记录浏览历史（同 URL 更新时间，标题取当前标签标题） */
    fun recordHistory(tabId: String, url: String) {
        if (UrlUtils.isHome(url)) return
        val title = _tabs.value.firstOrNull { it.id == tabId }?.title.orEmpty()
        container.history.add(url, title)
    }

    /** target=_blank / window.open：建新标签并把 WebView 交给 transport */
    fun handleCreateWindow(sourceTabId: String, resultMsg: android.os.Message): Boolean {
        val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
        val srcTab = _tabs.value.firstOrNull { it.id == sourceTabId }
        val state = TabState(
            id = UUID.randomUUID().toString(),
            url = "",
            title = "",
            desktopMode = srcTab?.desktopMode ?: container.settings.current.desktopModeDefault,
        )
        val wv = createWebView(state)
        _tabs.value = _tabs.value + state
        _currentId.value = state.id
        transport.webView = wv
        resultMsg.sendToTarget()
        syncWebView()
        persist()
        return true
    }

    /** window.close() 对应的标签关闭 */
    fun closeWindow(wv: WebView) {
        (wv.tag as? String)?.let { closeTab(it) }
    }

    fun onRenderGone(tabId: String) {
        val wv = webViews.remove(tabId) ?: return
        (wv.parent as? ViewGroup)?.removeView(wv)
        runCatching { wv.destroy() }
        if (tabId == _currentId.value) syncWebView()
    }

    // ---------- 设置联动 ----------

    private fun applySettings(s: BrowserSettings) {
        val dark = effectiveDark(s)
        val uaKey = "${s.uaPresetId}|${s.customUserAgent}"
        val first = lastAppliedUa == null
        val uaChanged = !first && uaKey != lastAppliedUa
        val darkChanged = lastAppliedDark != null && dark != lastAppliedDark
        val zoomChanged = lastAppliedZoom != null && s.pageZoomPercent != lastAppliedZoom
        val adChanged = lastAppliedAd != null && s.adBlockKey() != lastAppliedAd
        val devChanged = lastAppliedDev != null && s.devTools != lastAppliedDev
        val remoteChanged = s.remoteDebug != lastAppliedRemote // 首次也同步（null != 值）
        lastAppliedUa = uaKey
        lastAppliedDark = dark
        lastAppliedZoom = s.pageZoomPercent
        lastAppliedAd = s.adBlockKey()
        lastAppliedDev = s.devTools
        lastAppliedRemote = s.remoteDebug
        if (remoteChanged) WebView.setWebContentsDebuggingEnabled(s.remoteDebug)
        if (webViews.isEmpty()) return
        webViews.values.forEach { wv ->
            WebViewFactory.applyLiveSettings(wv, s)
            wv.evaluateJavascript(JsScripts.fontCss(s.fontFamily), null)
            if (zoomChanged) {
                // CSS zoom 即时生效，无需刷新
                wv.evaluateJavascript(JsScripts.zoomCss(s.pageZoomPercent), null)
            }
            if (adChanged) {
                // 拦截对后续请求立即生效；隐藏 CSS 重注入（关→开或白名单变化时移除/添加）
                val tab = tabOf(wv)
                if (tab != null && !UrlUtils.isHome(tab.url)) injectAdHide(wv, tab.url)
            }
            if (devChanged) injectDevTools(wv, s.devTools)
            val tab = tabOf(wv)
            if (uaChanged) {
                WebViewFactory.applyUserAgent(context, wv, s, tab?.desktopMode == true)
                if (tab != null && !UrlUtils.isHome(tab.url)) wv.reload()
            }
            if (darkChanged) {
                WebViewFactory.applyDarkening(wv, dark)
                if (tab != null && !UrlUtils.isHome(tab.url)) wv.reload()
            }
        }
    }

    private fun tabOf(wv: WebView): TabState? =
        (wv.tag as? String)?.let { id -> _tabs.value.firstOrNull { it.id == id } }

    private fun effectiveDark(s: BrowserSettings): Boolean = when (s.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.FOLLOW_SYSTEM -> isSystemDark()
    }

    private fun isSystemDark(): Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    // ---------- 持久化 ----------

    internal fun persist() {
        val list = _tabs.value
        val idx = list.indexOfFirst { it.id == _currentId.value }.coerceAtLeast(0)
        container.tabSessions.save(
            list.map { com.felix021.navigateur.data.TabSessionStore.SavedTab(it.url, it.title, it.desktopMode) },
            idx,
        )
    }

    private fun restore() {
        val session = container.tabSessions.restore()
        if (session == null || session.tabs.isEmpty()) {
            newTab()
            return
        }
        _tabs.value = session.tabs.map {
            TabState(
                id = UUID.randomUUID().toString(),
                url = it.url,
                title = if (UrlUtils.isHome(it.url)) "起始页" else it.title,
                desktopMode = it.desktopMode,
            )
        }
        _currentId.value = _tabs.value[session.selectedIndex.coerceIn(0, _tabs.value.size - 1)].id
        persist()
    }
}
