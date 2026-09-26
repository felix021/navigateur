package com.felix021.navigateur.ui.screen

import android.content.res.Configuration
import android.net.http.SslCertificate
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.ZoomIn
import com.felix021.navigateur.ui.component.AppDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.felix021.navigateur.browser.TabState
import com.felix021.navigateur.ui.BrowserController
import com.felix021.navigateur.ui.component.StatusStripTop
import com.felix021.navigateur.ui.component.StatusStripVertical
import com.felix021.navigateur.ui.Screen
import com.felix021.navigateur.util.UrlUtils
import java.util.Date
import androidx.compose.ui.res.stringResource
import com.felix021.navigateur.R

@Composable
fun BrowserScreen(controller: BrowserController) {
    val tabs by controller.tabManager.tabs.collectAsState()
    val currentId by controller.tabManager.currentId.collectAsState()
    val current = tabs.firstOrNull { it.id == currentId }
    val nav = controller.tabManager.nav.value
    val bookmarks by controller.container.bookmarks.bookmarks.collectAsState()
    val bookmarked = current != null && bookmarks.any { it.url == current.url }
    val settings by controller.container.settings.settings.collectAsState()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // 横屏默认全屏：隐藏状态栏 + 手势指示条（底部安全区开关打开时保留导航条）。
    // 部分 ROM 会在窗口焦点变化后恢复系统栏，ON_RESUME 时重放
    DisposableEffect(isLandscape, settings.landscapeFullscreen, settings.landscapeBottomSafeArea) {
        val activity = controller.activity
        val window = activity.window
        val insetsCtrl = WindowCompat.getInsetsController(window, window.decorView)
        fun applyInsets() {
            if (isLandscape && settings.landscapeFullscreen) {
                insetsCtrl.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsCtrl.hide(WindowInsetsCompat.Type.statusBars())
                if (settings.landscapeBottomSafeArea) {
                    insetsCtrl.show(WindowInsetsCompat.Type.navigationBars())
                } else {
                    insetsCtrl.hide(WindowInsetsCompat.Type.navigationBars())
                }
            } else {
                insetsCtrl.show(WindowInsetsCompat.Type.statusBars())
                insetsCtrl.show(WindowInsetsCompat.Type.navigationBars())
            }
        }
        applyInsets()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) applyInsets()
        }
        activity.lifecycle.addObserver(observer)
        onDispose { activity.lifecycle.removeObserver(observer) }
    }

    // 返回键始终接管：有页面历史则后退，否则询问退出
    BackHandler {
        if (nav.first) controller.tabManager.goBack()
        else controller.exitConfirm.value = true
    }
    if (controller.exitConfirm.value) {
        ExitConfirmDialog(
            onConfirm = {
                controller.exitConfirm.value = false
                controller.exitApp()
            },
            onDismiss = { controller.exitConfirm.value = false },
        )
    }

    // 横屏全屏时按页面左缘取色（PixelCopy），自绘状态条用它配色
    val density = LocalDensity.current
    val cutLeft = WindowInsets.displayCutout.getLeft(density, LayoutDirection.Ltr)
    LaunchedEffect(isLandscape, current?.url, settings.themeMode, settings.landscapeFullscreen) {
        if (isLandscape && settings.landscapeFullscreen) {
            kotlinx.coroutines.delay(700)
            controller.refreshEdgeColor(cutLeft.coerceAtLeast(0))
        }
    }
    val edge = controller.edgeColor.value
    val statusBg = edge?.let { Color(it) } ?: Color(0xF0101014)
    val statusFg = if ((edge?.let { Color(it).luminance() } ?: 0f) > 0.6f) {
        Color(0xE6101010)
    } else {
        Color.White
    }

    // 竖屏地址栏（浮层）的可见区域：点区域外 = 退出地址栏编辑态
    val omniboxBounds = remember { mutableStateOf<Rect?>(null) }
    val focusManager = LocalFocusManager.current
    Box(
        Modifier
            .fillMaxSize()
            // Initial pass 观察手势（不消费，不影响 WebView / 按钮交互）：
            // 在地址栏以外按下并抬手 = 点击了别处，退出地址栏编辑态。
            // 判定放在抬手而不是按下：按下就清焦会让输入建议面板当帧消失、点击落空
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    var downPos: androidx.compose.ui.geometry.Offset? = null
                    while (true) {
                        val ev = awaitPointerEvent(PointerEventPass.Initial)
                        val down = ev.changes.firstOrNull { it.pressed && !it.previousPressed }
                        if (down != null) {
                            downPos = down.position
                            continue
                        }
                        val up = ev.changes.firstOrNull { !it.pressed && it.previousPressed }
                        if (up != null && downPos != null) {
                            val pos = downPos
                            downPos = null
                            if (omniboxBounds.value?.contains(pos) != true) {
                                focusManager.clearFocus()
                            }
                        }
                    }
                }
            },
    ) {
        if (isLandscape) {
            LandscapeBrowser(
                controller, current, bookmarked, nav, tabs.size,
                settings.landscapeToolbarSide, settings.landscapeFullscreen,
                statusBg, statusFg, omniboxBounds,
            )
        } else {
            PortraitBrowser(controller, current, bookmarked, nav, tabs.size, omniboxBounds)
        }
    }
}

// ---------- 竖屏：顶部地址栏 + 底部工具条 ----------

@Composable
private fun PortraitBrowser(
    controller: BrowserController,
    current: TabState?,
    bookmarked: Boolean,
    nav: Pair<Boolean, Boolean>,
    tabCount: Int,
    omniboxBounds: androidx.compose.runtime.MutableState<Rect?>,
) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Omnibox(
            controller = controller,
            url = current?.url.orEmpty(),
            onSubmit = { controller.loadOrSearch(it) },
            // 胶囊外形需要四周留白，避免通栏灰条贴边
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            onBounds = { omniboxBounds.value = it },
        )
        if (current?.loading == true) {
            LinearProgressIndicator(
                progress = { current.progress.coerceIn(0, 100) / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            BrowserContent(controller, current)
        }
        BottomBar(controller, current, bookmarked, nav, tabCount)
    }
}

// ---------- 横屏：侧边工具条 + 浮层地址栏 ----------

@Composable
private fun LandscapeBrowser(
    controller: BrowserController,
    current: TabState?,
    bookmarked: Boolean,
    nav: Pair<Boolean, Boolean>,
    tabCount: Int,
    toolbarSide: String,
    fullscreen: Boolean,
    statusBg: Color,
    statusFg: Color,
    omniboxBounds: androidx.compose.runtime.MutableState<Rect?>,
) {
    var urlPanelOpen by remember { mutableStateOf(false) }
    var certOpen by remember { mutableStateOf(false) }
    // 浮层地址栏关闭后没有可见地址栏，清掉 bounds（否则顶部区域会被误判为栏内）
    LaunchedEffect(urlPanelOpen) { if (!urlPanelOpen) omniboxBounds.value = null }
    var menuOpen by remember { mutableStateOf(false) }
    var zoomOpen by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    if (certOpen) {
        SiteInfoDialog(controller, onDismiss = { certOpen = false })
    }

    Box(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                // 打孔屏：内容与工具条避开左右 cutout 竖带
                .windowInsetsPadding(WindowInsets.displayCutout)
        ) {
        val content = @Composable {
            Box(Modifier.weight(1f).fillMaxHeight()) {
                BrowserContent(controller, current)
                if (current?.loading == true) {
                    LinearProgressIndicator(
                        progress = { current.progress.coerceIn(0, 100) / 100f },
                        modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).zIndex(2f),
                    )
                }
                // 浮层地址栏：输入 / 建议 / 复制 / 证书
                if (urlPanelOpen) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().zIndex(3f),
                        color = MaterialTheme.colorScheme.surface,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                        shadowElevation = 8.dp,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Omnibox(
                                controller = controller,
                                url = current?.url.orEmpty(),
                                onSubmit = {
                                    controller.loadOrSearch(it)
                                    urlPanelOpen = false
                                },
                                modifier = Modifier.weight(1f),
                                onBounds = { omniboxBounds.value = it },
                            )
                            IconButton(onClick = {
                                val url = current?.url.orEmpty()
                                if (url.isNotEmpty()) {
                                    clipboard.setText(AnnotatedString(url))
                                    Toast.makeText(controller.activity, controller.activity.getString(R.string.copied_url), Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.cd_copy_url))
                            }
                            IconButton(
                                enabled = current?.url?.startsWith("https://") == true,
                                onClick = { certOpen = true },
                            ) {
                                Icon(Icons.Filled.VerifiedUser, contentDescription = stringResource(R.string.cd_certificate))
                            }
                            IconButton(onClick = { urlPanelOpen = false }) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close))
                            }
                        }
                    }
                }
            }
        }
        val toolbar = @Composable {
            SideToolbar(
                controller = controller,
                current = current,
                bookmarked = bookmarked,
                nav = nav,
                tabCount = tabCount,
                onOpenUrl = { urlPanelOpen = true },
                menuOpen = menuOpen,
                onMenuOpenChange = { menuOpen = it },
                zoomOpen = zoomOpen,
                onZoomOpenChange = { zoomOpen = it },
            )
        }
        if (toolbarSide == "left") {
            toolbar()
            content()
        } else {
            content()
            toolbar()
        }
        }

        // 全屏时把状态信息画进 cutout 竖带（打孔屏的左右安全区），无 cutout 则顶部悬浮
        if (fullscreen) {
            val density = LocalDensity.current
            val cutLeft = WindowInsets.displayCutout.getLeft(density, LayoutDirection.Ltr)
            val cutRight = WindowInsets.displayCutout.getRight(density, LayoutDirection.Ltr)
            when {
                cutLeft >= CUTOUT_STRIP_MIN_PX -> StatusStripVertical(
                    Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight()
                        .width(with(density) { cutLeft.toDp() }),
                    bgColor = statusBg,
                    fgColor = statusFg,
                )
                cutRight >= CUTOUT_STRIP_MIN_PX -> StatusStripVertical(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(with(density) { cutRight.toDp() }),
                    bgColor = statusBg,
                    fgColor = statusFg,
                )
                else -> StatusStripTop(
                    Modifier.align(Alignment.TopCenter),
                    bgColor = statusBg,
                    fgColor = statusFg,
                )
            }
        }
    }
}

/** cutout 竖带至少这么宽（px 级别换算前）才放状态条，避免窄边硬塞 */
private const val CUTOUT_STRIP_MIN_PX = 40

/** 横屏侧边工具条 */
@Composable
private fun SideToolbar(
    controller: BrowserController,
    current: TabState?,
    bookmarked: Boolean,
    nav: Pair<Boolean, Boolean>,
    tabCount: Int,
    onOpenUrl: () -> Unit,
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    zoomOpen: Boolean,
    onZoomOpenChange: (Boolean) -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, tonalElevation = 2.dp) {
        Column(
            Modifier
                .fillMaxHeight()
                .width(52.dp)
                // 上下留空：弧形屏边缘不排按钮，避免误触/遮挡
                .padding(vertical = 14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconButton(onClick = onOpenUrl) {
                Icon(Icons.Filled.Language, contentDescription = stringResource(R.string.cd_address))
            }
            IconButton(enabled = nav.first, onClick = { controller.tabManager.goBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
            }
            IconButton(enabled = nav.second, onClick = { controller.tabManager.goForward() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.cd_forward))
            }
            IconButton(onClick = { controller.tabManager.goHome() }) {
                Icon(Icons.Filled.Home, contentDescription = stringResource(R.string.cd_home))
            }
            IconButton(
                enabled = current != null && !UrlUtils.isHome(current.url),
                onClick = { controller.toggleBookmark() },
            ) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = stringResource(R.string.cd_bookmark),
                    tint = if (bookmarked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { controller.screen.value = Screen.Tabs }) {
                BadgedBox(badge = { Badge { Text(tabCount.toString()) } }) {
                    Icon(Icons.Filled.Tab, contentDescription = stringResource(R.string.cd_tabs))
                }
            }
            Box {
                IconButton(onClick = { onMenuOpenChange(true) }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.cd_menu))
                }
                BrowserMenuContent(
                    controller = controller,
                    current = current,
                    menuOpen = menuOpen,
                    onDismiss = { onMenuOpenChange(false) },
                    zoomOpen = zoomOpen,
                    onZoomOpen = onZoomOpenChange,
                )
            }
        }
    }
}

// ---------- 共享组件 ----------

/** WebView 容器 + 起始页 + 错误页 */
@Composable
private fun BrowserContent(controller: BrowserController, current: TabState?) {
    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                FrameLayout(ctx).also { controller.tabManager.webContainer = it }
            },
            update = { controller.tabManager.syncWebView() },
            modifier = Modifier.matchParentSize(),
        )
        LaunchedEffect(current?.id) { controller.tabManager.syncWebView() }
        if (current == null || UrlUtils.isHome(current.url)) {
            StartPage(controller, Modifier.matchParentSize())
        }
        current?.error?.let { err ->
            ErrorOverlay(err) { controller.tabManager.reloadCurrent() }
        }
    }
}

/** ⋮ 菜单内容（竖屏底栏 / 横屏侧栏共用） */
@Composable
private fun BrowserMenuContent(
    controller: BrowserController,
    current: TabState?,
    menuOpen: Boolean,
    onDismiss: () -> Unit,
    zoomOpen: Boolean,
    onZoomOpen: (Boolean) -> Unit,
) {
    val settings by controller.container.settings.settings.collectAsState()
    if (zoomOpen) {
        ZoomDialog(
            zoomPercent = settings.pageZoomPercent,
            onZoom = { pct ->
                controller.container.settings.update { s -> s.copy(pageZoomPercent = pct) }
            },
            onDismiss = { onZoomOpen(false) },
        )
    }
    com.felix021.navigateur.ui.component.AppDropdownMenu(
        expanded = menuOpen,
        onDismissRequest = onDismiss,
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.tab_new)) },
            leadingIcon = { Icon(Icons.Filled.Add, null) },
            onClick = {
                onDismiss()
                controller.tabManager.newTab()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_reload)) },
            leadingIcon = { Icon(Icons.Filled.Refresh, null) },
            onClick = {
                onDismiss()
                controller.tabManager.reloadCurrent()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_desktop_mode)) },
            leadingIcon = { Icon(Icons.Filled.DesktopWindows, null) },
            trailingIcon = if (current?.desktopMode == true) {
                { Icon(Icons.Filled.Check, null) }
            } else null,
            onClick = {
                onDismiss()
                current?.let { controller.tabManager.setDesktopMode(it.id, !it.desktopMode) }
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_zoom, settings.pageZoomPercent)) },
            leadingIcon = { Icon(Icons.Filled.ZoomIn, null) },
            onClick = {
                onDismiss()
                onZoomOpen(true)
            },
        )
        // 代理快切：直连 → 各出口 → 自动 → 直连
        run {
            val proxy = com.felix021.navigateur.data.ProxyRepository.fromJson(settings.proxyJson)
            val chain = buildList {
                add("direct"); proxy.profiles.forEach { add(it.id) }; add("auto")
            }
            val cur = if (proxy.mode in chain) proxy.mode else "direct"
            val label = when (cur) {
                "direct" -> stringResource(R.string.label_direct)
                "auto" -> stringResource(R.string.label_auto)
                else -> proxy.profiles.firstOrNull { it.id == cur }?.name
                    ?: stringResource(R.string.label_direct)
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_proxy, label)) },
                leadingIcon = { Icon(Icons.Filled.Language, null) },
                onClick = {
                    onDismiss()
                    val next = chain[(chain.indexOf(cur) + 1) % chain.size]
                    controller.container.settings.update { s ->
                        val p = com.felix021.navigateur.data.ProxyRepository.fromJson(s.proxyJson)
                        s.copy(proxyJson = com.felix021.navigateur.data.ProxyRepository.toJson(
                            p.copy(mode = next, autoProfileId = p.autoProfileId.ifEmpty { p.profiles.firstOrNull()?.id ?: "" }),
                        ))
                    }
                },
            )
        }
        // 当前站点广告拦截开关（仅广告拦截开启时展示，避免误导）
        if (settings.adBlockEnabled && current != null && !UrlUtils.isHome(current.url)) {
            val host = UrlUtils.hostOf(current.url)
            val allowlisted = settings.adBlockAllowlist.any { host == it || host.endsWith(".$it") }
            DropdownMenuItem(
                text = {
                    Text(if (allowlisted) stringResource(R.string.adblock_block_site)
                        else stringResource(R.string.adblock_unblock_site))
                },
                leadingIcon = { Icon(Icons.Filled.Block, null) },
                onClick = {
                    onDismiss()
                    if (host.isNotEmpty()) {
                        controller.container.settings.update { s ->
                            s.copy(
                                adBlockAllowlist =
                                if (allowlisted) s.adBlockAllowlist - host else s.adBlockAllowlist + host,
                            )
                        }
                        controller.tabManager.reloadCurrent()
                    }
                },
            )
        }
        DropdownMenuItem(
            text = { Text(stringResource(R.string.bookmarks_title)) },
            leadingIcon = { Icon(Icons.Filled.Bookmarks, null) },
            onClick = {
                onDismiss()
                controller.screen.value = Screen.Bookmarks
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_settings)) },
            leadingIcon = { Icon(Icons.Filled.Settings, null) },
            onClick = {
                onDismiss()
                controller.screen.value = Screen.Settings
            },
        )
    }
}

@Composable
private fun ExitConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.exit_title)) },
        text = { Text(stringResource(R.string.exit_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.exit_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/** 站点信息：host / URL / 连接安全状态 + 证书详情（来自当前 WebView 主资源证书） */
@Composable
private fun SiteInfoDialog(controller: BrowserController, onDismiss: () -> Unit) {
    val cert: SslCertificate? = controller.tabManager.currentWebView?.certificate
    val url = controller.tabManager.current?.url.orEmpty()
    val host = UrlUtils.hostOf(url).ifEmpty { url }
    val secure = url.startsWith("https://")
    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text(host.ifEmpty { stringResource(R.string.cert_title) }) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(
                        if (secure) R.string.site_info_secure else R.string.site_info_not_secure,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (secure) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(8.dp))
                if (cert != null) {
                    CertRow(stringResource(R.string.cert_issued_to), certName(cert, byIssuer = false))
                    CertRow(stringResource(R.string.cert_issued_by), certName(cert, byIssuer = true))
                    CertRow(stringResource(R.string.cert_valid_from), formatTime(cert.validNotBeforeDate))
                    CertRow(stringResource(R.string.cert_valid_to), formatTime(cert.validNotAfterDate))
                } else {
                    Text(
                        stringResource(R.string.cert_none),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

/**
 * 证书主体/颁发者 DN 文本。
 * API 29 起 getIssuedTo/By 返回 DName（toString 未覆写，会显示成对象地址，须取 getDName）；
 * 老 API 返回 String，编译期已绑定新签名，运行时直接调会 NoSuchMethodError —— 走反射。
 */
private fun certName(cert: SslCertificate, byIssuer: Boolean): String = runCatching {
    if (android.os.Build.VERSION.SDK_INT >= 29) {
        val dn = if (byIssuer) cert.issuedBy else cert.issuedTo
        dn.dName
    } else {
        val m = SslCertificate::class.java.getMethod(if (byIssuer) "getIssuedBy" else "getIssuedTo")
        m.invoke(cert)?.toString() ?: ""
    }
}.getOrDefault("")

private fun formatTime(date: Date?): String =
    date?.let { java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(it) } ?: "—"

@Composable
private fun CertRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

private data class OmniSuggestion(val url: String, val title: String, val fromBookmark: Boolean)

@Composable
private fun Omnibox(
    controller: BrowserController,
    url: String,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
    onBounds: ((Rect) -> Unit)? = null,
) {
    var value by remember { mutableStateOf(TextFieldValue("")) }
    var showSiteInfo by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val focusManager = LocalFocusManager.current
    val history by controller.container.history.history.collectAsState()
    val bookmarks by controller.container.bookmarks.bookmarks.collectAsState()
    // 编辑态多行：上限半屏；建议面板贴着地址栏底部（高度动态）
    var boxHeightPx by remember { mutableIntStateOf(0) }
    val maxEditHeight = with(LocalDensity.current) {
        (LocalConfiguration.current.screenHeightDp / 2).dp
    }
    val boxHeightDp = with(LocalDensity.current) { boxHeightPx.toDp() }

    LaunchedEffect(url) {
        if (!focused) {
            val t = if (UrlUtils.isHome(url)) "" else UrlUtils.pretty(url)
            value = TextFieldValue(t, selection = TextRange(t.length))
        }
    }

    // 聚焦即全选：输入新网址直接覆盖，避免光标落在旧文本中间造成拼接
    LaunchedEffect(focused) {
        if (focused) value = value.copy(selection = TextRange(0, value.text.length))
    }

    // 输入建议：书签命中置顶（标星），其后按访问时间倒序的历史
    val suggestions = if (focused && value.text.isNotBlank()) {
        val q = value.text.trim()
        val bm = bookmarks.filter {
            it.url.contains(q, true) || it.title.contains(q, true)
        }.take(3).map { OmniSuggestion(it.url, it.title, fromBookmark = true) }
        val hi = history.filter {
            it.url.contains(q, true) || it.title.contains(q, true)
        }.take(6).map { OmniSuggestion(it.url, it.title, fromBookmark = false) }
        (bm + hi).distinctBy { it.url }.take(6)
    } else emptyList()

    Box(
        modifier.then(
            if (onBounds != null) {
                Modifier.onGloballyPositioned { onBounds(it.boundsInRoot()) }
            } else Modifier,
        ),
    ) {
        if (showSiteInfo) {
            SiteInfoDialog(controller, onDismiss = { showSiteInfo = false })
        }
        // 输入建议面板，浮在页面内容之上
        if (suggestions.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .offset(y = boxHeightDp + 4.dp)
                    .zIndex(1f),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                tonalElevation = 3.dp,
                shadowElevation = 6.dp,
            ) {
                Column {
                    suggestions.forEach { s ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    focusManager.clearFocus()
                                    onSubmit(s.url)
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                if (s.fromBookmark) Icons.Filled.Star else Icons.Filled.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    s.title.ifBlank { s.url },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    UrlUtils.pretty(s.url),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
        Surface(
            color = if (focused) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.onSizeChanged { boxHeightPx = it.height },
        ) {
            BasicTextField(
                value = value,
                onValueChange = { v ->
                    // 多行编辑态下物理键盘 Enter 会插换行：当作「前往」提交
                    if (v.text.contains('\n')) {
                        val t = v.text.replace("\n", "")
                        if (t.isNotBlank()) onSubmit(t)
                        focusManager.clearFocus()
                    } else {
                        value = v
                    }
                },
                singleLine = !focused,
                maxLines = if (focused) 8 else 1,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                interactionSource = interaction,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(
                    onGo = {
                        if (value.text.isNotBlank()) onSubmit(value.text)
                        focusManager.clearFocus()
                    }
                ),
                decorationBox = { inner ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxEditHeight)
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = if (focused) Alignment.Top else Alignment.CenterVertically,
                    ) {
                        // 站点信息入口：点击只弹信息，不进入编辑态
                        IconButton(
                            onClick = {
                                focusManager.clearFocus()
                                showSiteInfo = true
                            },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                if (!focused && url.startsWith("https://")) Icons.Filled.Lock
                                else Icons.Filled.Search,
                                contentDescription = stringResource(R.string.cd_certificate),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            if (value.text.isEmpty()) {
                                Text(
                                    if (UrlUtils.isHome(url)) stringResource(R.string.search_or_url) else "",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            inner()
                        }
                        if (focused && value.text.isNotEmpty()) {
                            IconButton(onClick = { value = TextFieldValue("") }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_clear_input), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                },
            )
        }
    }
}

/** 页面缩放对话框：滑块实时改整体缩放 */
@Composable
private fun ZoomDialog(
    zoomPercent: Int,
    onZoom: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.zoom_title)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "$zoomPercent%",
                    style = MaterialTheme.typography.headlineMedium,
                )
                androidx.compose.material3.Slider(
                    value = zoomPercent.toFloat(),
                    onValueChange = { onZoom(it.toInt().coerceIn(50, 200)) },
                    valueRange = 50f..200f,
                )
                Text(
                    stringResource(R.string.zoom_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) }
        },
    )
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
    var zoomOpen by remember { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, tonalElevation = 2.dp) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().height(52.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(enabled = nav.first, onClick = { controller.tabManager.goBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
            }
            IconButton(enabled = nav.second, onClick = { controller.tabManager.goForward() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.cd_forward))
            }
            IconButton(onClick = { controller.tabManager.goHome() }) {
                Icon(Icons.Filled.Home, contentDescription = stringResource(R.string.cd_home))
            }
            Spacer(Modifier.weight(1f))
            IconButton(
                enabled = current != null && !UrlUtils.isHome(current.url),
                onClick = { controller.toggleBookmark() },
            ) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = stringResource(R.string.cd_bookmark),
                    tint = if (bookmarked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { controller.screen.value = Screen.Tabs }) {
                BadgedBox(badge = { Badge { Text(tabCount.toString()) } }) {
                    Icon(Icons.Filled.Tab, contentDescription = stringResource(R.string.cd_tabs))
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.cd_menu))
                }
                BrowserMenuContent(
                    controller = controller,
                    current = current,
                    menuOpen = menuOpen,
                    onDismiss = { menuOpen = false },
                    zoomOpen = zoomOpen,
                    onZoomOpen = { zoomOpen = it },
                )
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
        Text(stringResource(R.string.page_load_failed), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}
