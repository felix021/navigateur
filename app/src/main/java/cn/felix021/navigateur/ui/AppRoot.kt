package cn.felix021.navigateur.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import cn.felix021.navigateur.data.ThemeMode
import cn.felix021.navigateur.ui.component.SavePasswordDialog
import cn.felix021.navigateur.ui.screen.BookmarksScreen
import cn.felix021.navigateur.ui.screen.BrowserScreen
import cn.felix021.navigateur.ui.screen.SettingsScreen
import cn.felix021.navigateur.ui.screen.TabsScreen

@Composable
fun AppRoot(controller: BrowserController) {
    val settings by controller.container.settings.settings.collectAsState()
    val dark = when (settings.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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
