package cn.felix021.navigateur.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
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
                Screen.Tabs -> TabsScreen(controller)
                Screen.Bookmarks -> BookmarksScreen(controller)
                Screen.Settings -> SettingsScreen(controller)
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
