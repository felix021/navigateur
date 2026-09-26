package com.felix021.puff.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.felix021.puff.data.ThemeMode
import com.felix021.puff.ui.component.SavePasswordDialog
import com.felix021.puff.ui.screen.BookmarksScreen
import com.felix021.puff.ui.screen.BrowserScreen
import com.felix021.puff.ui.screen.SettingsScreen
import com.felix021.puff.ui.screen.TabsScreen

@Composable
fun AppRoot(controller: BrowserController) {
    val settings by controller.container.settings.settings.collectAsState()
    val dark = when (settings.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(colorScheme = accentColorScheme(settings.accent, settings.accentHue, dark)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            // 子页面（标签/书签/设置）返回 → 回浏览页；浏览页自身由 BrowserScreen 的
            // BackHandler 处理（页面后退 / 退出确认），内层 handler 优先消费
            if (controller.screen.value != Screen.Browser) {
                BackHandler { controller.screen.value = Screen.Browser }
            }
            when (controller.screen.value) {
                Screen.Browser -> BrowserScreen(controller)
                Screen.Tabs -> CutoutSafe { TabsScreen(controller) }
                Screen.Bookmarks -> CutoutSafe { BookmarksScreen(controller) }
                Screen.Settings -> CutoutSafe { SettingsScreen(controller) }
            }
            controller.savePrompt.value?.let { prompt ->
                SavePasswordDialog(
                    prompt = prompt,
                    onConfirm = controller::confirmSavePrompt,
                    onDismiss = controller::dismissSavePrompt,
                )
            }
        }
    }
}

/** 横屏打孔屏：子页面（标签/书签/设置）内容避开左右 cutout 竖带 */
@Composable
private fun CutoutSafe(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.displayCutout)) {
        content()
    }
}
