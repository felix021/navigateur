package com.felix021.navigateur.ui

import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.felix021.navigateur.R

/** 主题色预设：key 存设置；hue 供自定义滑块取初始值 */
data class AccentOption(val key: String, @StringRes val labelRes: Int, val hue: Float, val chroma: Float = 1f)

val ACCENTS = listOf(
    AccentOption("blue", R.string.accent_blue, hue = 215f),
    AccentOption("teal", R.string.accent_teal, hue = 174f),
    AccentOption("green", R.string.accent_green, hue = 130f),
    AccentOption("orange", R.string.accent_orange, hue = 32f),
    AccentOption("rose", R.string.accent_rose, hue = 340f),
    AccentOption("slate", R.string.accent_slate, hue = 205f, chroma = 0.45f),
)

fun accentLabelRes(key: String): Int =
    ACCENTS.firstOrNull { it.key == key }?.labelRes ?: R.string.accent_custom

/**
 * 由色相生成整套 Material3 配色（亮/暗）。
 * 六套预设与用户滑块自定义走同一条生成路径，保证视觉一致：
 * primary 实色、container 浅底深字、tertiary 偏移 +60° 做点缀色。
 */
fun accentColorScheme(accent: String, customHue: Float, dark: Boolean): ColorScheme {
    val opt = ACCENTS.firstOrNull { it.key == accent }
    val hue = opt?.hue ?: customHue
    val chroma = opt?.chroma ?: 1f

    fun c(h: Float, s: Float, l: Float) = Color.hsl(h, (s * chroma).coerceIn(0f, 1f), l)
    val t = (hue + 60f) % 360f

    return if (!dark) lightColorScheme(
        primary = c(hue, 0.62f, 0.36f),
        onPrimary = Color.White,
        primaryContainer = c(hue, 0.70f, 0.90f),
        onPrimaryContainer = c(hue, 0.70f, 0.16f),
        secondary = c(hue, 0.32f, 0.40f),
        onSecondary = Color.White,
        secondaryContainer = c(hue, 0.28f, 0.88f),
        onSecondaryContainer = c(hue, 0.32f, 0.18f),
        tertiary = c(t, 0.42f, 0.40f),
        onTertiary = Color.White,
        tertiaryContainer = c(t, 0.38f, 0.90f),
        onTertiaryContainer = c(t, 0.42f, 0.16f),
    ) else darkColorScheme(
        primary = c(hue, 0.52f, 0.72f),
        onPrimary = c(hue, 0.75f, 0.14f),
        primaryContainer = c(hue, 0.45f, 0.30f),
        onPrimaryContainer = c(hue, 0.55f, 0.90f),
        secondary = c(hue, 0.25f, 0.72f),
        onSecondary = c(hue, 0.35f, 0.14f),
        secondaryContainer = c(hue, 0.22f, 0.28f),
        onSecondaryContainer = c(hue, 0.25f, 0.90f),
        tertiary = c(t, 0.35f, 0.70f),
        onTertiary = c(t, 0.45f, 0.14f),
        tertiaryContainer = c(t, 0.30f, 0.28f),
        onTertiaryContainer = c(t, 0.35f, 0.90f),
    )
}

/** 主题色选择对话框里的小圆点颜色（与实际 scheme 的 primary 一致） */
@Composable
fun accentPreview(accent: String, customHue: Float, dark: Boolean): Color =
    accentColorScheme(accent, customHue, dark).primary

/** 当前主题模式对应的暗色态（AppRoot 与设置预览共用） */
@Composable
fun isDarkTheme(mode: com.felix021.navigateur.data.ThemeMode): Boolean = when (mode) {
    com.felix021.navigateur.data.ThemeMode.DARK -> true
    com.felix021.navigateur.data.ThemeMode.LIGHT -> false
    com.felix021.navigateur.data.ThemeMode.FOLLOW_SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
}
