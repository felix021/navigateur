package cn.felix021.navigateur.browser

import android.content.Context
import android.os.Build
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import cn.felix021.navigateur.data.BrowserSettings

/** 所有 WebView 的 WebSettings 集中配置 */
object WebViewFactory {

    /** 桌面 Chrome UA（Linux 桌面） */
    const val DESKTOP_UA =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"

    fun configure(context: Context, wv: WebView, s: BrowserSettings, desktopMode: Boolean, dark: Boolean) {
        val ws = wv.settings
        ws.javaScriptEnabled = true
        ws.domStorageEnabled = true
        ws.setSupportZoom(true)
        ws.builtInZoomControls = true
        ws.displayZoomControls = false
        ws.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        ws.allowFileAccess = false
        ws.allowContentAccess = false
        ws.javaScriptCanOpenWindowsAutomatically = true
        ws.setSupportMultipleWindows(false)
        if (Build.VERSION.SDK_INT >= 26) ws.safeBrowsingEnabled = true

        // 桌面/移动模式：UA 是唯一实质开关，两种模式下 viewport 都交给页面
        ws.useWideViewPort = true
        ws.loadWithOverviewMode = true
        applyUserAgent(context, wv, s, desktopMode)

        ws.textZoom = s.textZoomPercent
        applyDarkening(wv, dark)
    }

    /** 无导航副作用的配置项（可对已加载页面随时应用） */
    fun applyLiveSettings(wv: WebView, s: BrowserSettings) {
        wv.settings.textZoom = s.textZoomPercent
    }

    fun applyUserAgent(context: Context, wv: WebView, s: BrowserSettings, desktopMode: Boolean) {
        wv.settings.userAgentString = when {
            desktopMode -> DESKTOP_UA
            s.customUserAgent.isNotBlank() -> s.customUserAgent
            else -> WebSettings.getDefaultUserAgent(context).replace("; wv", ";") // 去 WebView 标记
        }
    }

    /** 网页暗色：优先算法暗色（targetSdk>=33 + Activity night + WebView 支持），回退 legacy forceDark */
    fun applyDarkening(wv: WebView, dark: Boolean) {
        val ws = wv.settings
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(ws, dark)
        } else if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            @Suppress("DEPRECATION")
            WebSettingsCompat.setForceDark(ws, if (dark) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF)
        } else if (Build.VERSION.SDK_INT >= 29) {
            @Suppress("DEPRECATION")
            ws.forceDark = if (dark) WebSettings.FORCE_DARK_ON else WebSettings.FORCE_DARK_OFF
        }
        // API<29 且 WebView 旧：无原生方案，保持亮色
    }
}
