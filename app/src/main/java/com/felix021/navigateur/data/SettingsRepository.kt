package com.felix021.navigateur.data

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ThemeMode { FOLLOW_SYSTEM, LIGHT, DARK }

data class BrowserSettings(
    val homepage: String = "about:home",
    val searchEngineId: String = "duckduckgo",
    val desktopModeDefault: Boolean = false,
    /** UA 预设 id，见 UaPresets；custom 时使用 customUserAgent */
    val uaPresetId: String = "default",
    val customUserAgent: String = "",
    /** 空 = 跟随网站；否则为 CSS font-family 值（sans-serif / serif / monospace） */
    val fontFamily: String = "",
    /** 页面整体缩放百分比（CSS zoom，非仅字体），50–200 */
    val pageZoomPercent: Int = 100,
    val themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    val savePasswords: Boolean = true,
    /** 横屏侧边工具栏位置：right / left */
    val landscapeToolbarSide: String = "right",
    /** 横屏默认全屏（隐藏系统状态栏 + 手势指示条） */
    val landscapeFullscreen: Boolean = true,
    /** 横屏底部安全区：显示系统导航条并避让（关闭则完全全屏） */
    val landscapeBottomSafeArea: Boolean = false,
    /** 广告拦截总开关 */
    val adBlockEnabled: Boolean = true,
    /** 广告拦截站点白名单（host，在这些站点完全不拦截） */
    val adBlockAllowlist: Set<String> = emptySet(),
    /** 远程调试（chrome://inspect / CDP）：默认关，需要时打开（安全纵深，socket 仅 adb 可达） */
    val remoteDebug: Boolean = false,
    /** 页面内开发者工具（eruda：Console/Elements/Network 等） */
    val devTools: Boolean = false,
)

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<BrowserSettings> = _settings

    /** 当前设置的便捷读取 */
    val current: BrowserSettings get() = _settings.value

    init {
        applyNightMode(_settings.value.themeMode)
    }

    fun update(transform: (BrowserSettings) -> BrowserSettings) {
        val prev = _settings.value
        val next = transform(prev)
        if (next == prev) return
        persist(next)
        _settings.value = next
        if (next.themeMode != prev.themeMode) applyNightMode(next.themeMode)
    }

    private fun read(): BrowserSettings = BrowserSettings(
        homepage = prefs.getString(KEY_HOME, "about:home") ?: "about:home",
        searchEngineId = prefs.getString(KEY_ENGINE, "duckduckgo") ?: "duckduckgo",
        desktopModeDefault = prefs.getBoolean(KEY_DESKTOP, false),
        uaPresetId = prefs.getString(KEY_UA_PRESET, "default") ?: "default",
        customUserAgent = prefs.getString(KEY_UA, "") ?: "",
        fontFamily = prefs.getString(KEY_FONT, "") ?: "",
        pageZoomPercent = prefs.getInt(KEY_ZOOM, 100).coerceIn(50, 200),
        themeMode = runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.FOLLOW_SYSTEM.name)!!)
        }.getOrDefault(ThemeMode.FOLLOW_SYSTEM),
        savePasswords = prefs.getBoolean(KEY_SAVEPW, true),
        landscapeToolbarSide = if (prefs.getString(KEY_LAND_SIDE, "right") == "left") "left" else "right",
        landscapeFullscreen = prefs.getBoolean(KEY_LAND_FULLSCREEN, true),
        landscapeBottomSafeArea = prefs.getBoolean(KEY_LAND_BOTTOM, false),
        adBlockEnabled = prefs.getBoolean(KEY_ADBLOCK, true),
        adBlockAllowlist = prefs.getString(KEY_ADBLOCK_ALLOW, "")!!.split(',')
            .filter { it.isNotBlank() }.toSet(),
        remoteDebug = prefs.getBoolean(KEY_REMOTE_DEBUG, false),
        devTools = prefs.getBoolean(KEY_DEVTOOLS, false),
    )

    private fun persist(s: BrowserSettings) {
        prefs.edit()
            .putString(KEY_HOME, s.homepage)
            .putString(KEY_ENGINE, s.searchEngineId)
            .putBoolean(KEY_DESKTOP, s.desktopModeDefault)
            .putString(KEY_UA_PRESET, s.uaPresetId)
            .putString(KEY_UA, s.customUserAgent)
            .putString(KEY_FONT, s.fontFamily)
            .putInt(KEY_ZOOM, s.pageZoomPercent)
            .putString(KEY_THEME, s.themeMode.name)
            .putBoolean(KEY_SAVEPW, s.savePasswords)
            .putString(KEY_LAND_SIDE, s.landscapeToolbarSide)
            .putBoolean(KEY_LAND_FULLSCREEN, s.landscapeFullscreen)
            .putBoolean(KEY_LAND_BOTTOM, s.landscapeBottomSafeArea)
            .putBoolean(KEY_ADBLOCK, s.adBlockEnabled)
            .putString(KEY_ADBLOCK_ALLOW, s.adBlockAllowlist.sorted().joinToString(","))
            .putBoolean(KEY_REMOTE_DEBUG, s.remoteDebug)
            .putBoolean(KEY_DEVTOOLS, s.devTools)
            .commit() // commit 而非 apply：StringSet/apply 在部分 ROM 上不落盘，设置丢失
    }

    /** WebView 网页暗色依赖 Activity 资源处于 night 模式，这里同步 AppCompat 的全局夜间模式 */
    private fun applyNightMode(mode: ThemeMode) {
        if (LooperCheck.notMain()) return
        AppCompatDelegate.setDefaultNightMode(
            when (mode) {
                ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                ThemeMode.FOLLOW_SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    private object LooperCheck {
        fun notMain(): Boolean =
            android.os.Looper.myLooper() != android.os.Looper.getMainLooper()
    }

    private companion object {
        const val KEY_HOME = "homepage"
        const val KEY_ENGINE = "search_engine"
        const val KEY_DESKTOP = "desktop_default"
        const val KEY_UA_PRESET = "ua_preset"
        const val KEY_UA = "custom_ua"
        const val KEY_FONT = "font_family"
        const val KEY_ZOOM = "text_zoom"
        const val KEY_THEME = "theme_mode"
        const val KEY_SAVEPW = "save_passwords"
        const val KEY_LAND_SIDE = "landscape_side"
        const val KEY_LAND_FULLSCREEN = "landscape_fullscreen"
        const val KEY_LAND_BOTTOM = "landscape_bottom_safe"
        const val KEY_ADBLOCK = "adblock_enabled"
        const val KEY_ADBLOCK_ALLOW = "adblock_allowlist"
        const val KEY_REMOTE_DEBUG = "remote_debug"
        const val KEY_DEVTOOLS = "devtools"
    }
}
