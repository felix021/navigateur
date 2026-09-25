package com.felix021.navigateur.browser

import android.content.Context
import android.os.Build
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.felix021.navigateur.data.BrowserSettings
import com.felix021.navigateur.data.UaPresets

/** 所有 WebView 的 WebSettings 集中配置 */
object WebViewFactory {

    /** 桌面模式 UA（macOS Chrome） */
    const val DESKTOP_UA = UaPresets.DESKTOP_MAC_UA

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
        // 支持 window.open / target=_blank 经 onCreateWindow 开新标签
        ws.setSupportMultipleWindows(true)
        if (Build.VERSION.SDK_INT >= 26) ws.safeBrowsingEnabled = true

        // 桌面/移动模式：UA 是唯一实质开关，两种模式下 viewport 都交给页面
        ws.useWideViewPort = true
        ws.loadWithOverviewMode = true
        applyUserAgent(context, wv, s, desktopMode)

        applyDarkening(wv, dark)
    }

    /** 无导航副作用的配置项（可对已加载页面随时应用） */
    fun applyLiveSettings(wv: WebView, s: BrowserSettings) {
        // 整体缩放走 CSS zoom 注入（见 TabManager.applySettings），无原生 API
    }

    /**
     * UA 决策：预设（非默认）优先，不被桌面模式覆盖；
     * 仅预设为默认时，桌面模式开关切换 移动 ↔ macOS 桌面 UA。
     */
    fun applyUserAgent(context: Context, wv: WebView, s: BrowserSettings, desktopMode: Boolean) {
        val preset = UaPresets.byId(s.uaPresetId)
        wv.settings.userAgentString = when {
            preset.id == UaPresets.CUSTOM && s.customUserAgent.isNotBlank() -> s.customUserAgent
            preset.ua != null -> preset.ua
            desktopMode -> DESKTOP_UA
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
