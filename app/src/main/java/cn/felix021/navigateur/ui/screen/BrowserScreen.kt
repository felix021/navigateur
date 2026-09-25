package cn.felix021.navigateur.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.FrameLayout
import cn.felix021.navigateur.browser.TabState
import cn.felix021.navigateur.ui.BrowserController
import cn.felix021.navigateur.ui.Screen
import cn.felix021.navigateur.util.UrlUtils

@Composable
fun BrowserScreen(controller: BrowserController) {
    val tabs by controller.tabManager.tabs.collectAsState()
    val currentId by controller.tabManager.currentId.collectAsState()
    val current = tabs.firstOrNull { it.id == currentId }
    val nav = controller.tabManager.nav.value
    val bookmarks by controller.container.bookmarks.bookmarks.collectAsState()
    val bookmarked = current != null && bookmarks.any { it.url == current.url }

    BackHandler(enabled = nav.first) { controller.tabManager.goBack() }

    Column(Modifier.fillMaxSize()) {
        Omnibox(
            url = current?.url.orEmpty(),
            onSubmit = { controller.loadOrSearch(it) },
        )
        if (current?.loading == true) {
            LinearProgressIndicator(
                progress = { current.progress.coerceIn(0, 100) / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                factory = { ctx ->
                    FrameLayout(ctx).also { controller.tabManager.webContainer = it }
                },
                update = { controller.tabManager.syncWebView() },
                modifier = Modifier.matchParentSize(),
            )
            LaunchedEffect(currentId, tabs.size) { controller.tabManager.syncWebView() }
            if (current == null || UrlUtils.isHome(current.url)) {
                StartPage(controller, Modifier.matchParentSize())
            }
            current?.error?.let { err ->
                ErrorOverlay(err) { controller.tabManager.reloadCurrent() }
            }
        }
        BottomBar(controller, current, bookmarked, nav, tabs.size)
    }
}

@Composable
private fun Omnibox(url: String, onSubmit: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(url) {
        if (!focused) value = if (UrlUtils.isHome(url)) "" else UrlUtils.pretty(url)
    }

    Surface(color = MaterialTheme.colorScheme.surfaceVariant, tonalElevation = 2.dp) {
        BasicTextField(
            value = value,
            onValueChange = { value = it },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            interactionSource = interaction,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(
                onGo = {
                    if (value.isNotBlank()) onSubmit(value)
                    focusManager.clearFocus()
                }
            ),
            decorationBox = { inner ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (!focused && url.startsWith("https://")) Icons.Filled.Lock else Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(
                                if (UrlUtils.isHome(url)) "搜索或输入网址" else "",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        inner()
                    }
                    if (focused && value.isNotEmpty()) {
                        IconButton(onClick = { value = "" }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "清空", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun BottomBar(
    controller: BrowserController,
    current: TabState?,
    bookmarked: Boolean,
    nav: Pair<Boolean, Boolean>,
    tabCount: Int,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, tonalElevation = 2.dp) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().height(52.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(enabled = nav.first, onClick = { controller.tabManager.goBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "后退")
            }
            IconButton(enabled = nav.second, onClick = { controller.tabManager.goForward() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "前进")
            }
            IconButton(onClick = { controller.tabManager.goHome() }) {
                Icon(Icons.Filled.Home, contentDescription = "主页")
            }
            Spacer(Modifier.weight(1f))
            IconButton(
                enabled = current != null && !UrlUtils.isHome(current.url),
                onClick = { controller.toggleBookmark() },
            ) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = "收藏",
                    tint = if (bookmarked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { controller.screen.value = Screen.Tabs }) {
                BadgedBox(badge = { Badge { Text(tabCount.toString()) } }) {
                    Icon(Icons.Filled.Tab, contentDescription = "标签")
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "菜单")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("新建标签") },
                        leadingIcon = { Icon(Icons.Filled.Add, null) },
                        onClick = {
                            menuOpen = false
                            controller.tabManager.newTab()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("刷新") },
                        leadingIcon = { Icon(Icons.Filled.Refresh, null) },
                        onClick = {
                            menuOpen = false
                            controller.tabManager.reloadCurrent()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("桌面模式") },
                        leadingIcon = { Icon(Icons.Filled.DesktopWindows, null) },
                        trailingIcon = if (current?.desktopMode == true) {
                            { Icon(Icons.Filled.Check, null) }
                        } else null,
                        onClick = {
                            menuOpen = false
                            current?.let { controller.tabManager.setDesktopMode(it.id, !it.desktopMode) }
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("书签") },
                        leadingIcon = { Icon(Icons.Filled.Bookmarks, null) },
                        onClick = {
                            menuOpen = false
                            controller.screen.value = Screen.Bookmarks
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("设置") },
                        leadingIcon = { Icon(Icons.Filled.Settings, null) },
                        onClick = {
                            menuOpen = false
                            controller.screen.value = Screen.Settings
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorOverlay(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Text("页面加载失败", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("重试") }
    }
}
