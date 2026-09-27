package com.felix021.puff.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.felix021.puff.R
import com.felix021.puff.ui.BrowserController

/** 开源组件清单：name + 许可 + 详情页 */
private data class LicenseItem(val name: String, val detail: String, val url: String)

private val LICENSES = listOf(
    LicenseItem("eruda", "MIT", "https://github.com/liriliri/eruda"),
    LicenseItem("EasyList / EasyList China", "GPL-3.0 / CC BY-SA 3.0", "https://easylist.to/pages/licence.html"),
    LicenseItem("AndroidX (webkit / appcompat / core / lifecycle)", "Apache-2.0", "https://developer.android.com/jetpack/androidx"),
    LicenseItem("Jetpack Compose / Material3 / Material Icons", "Apache-2.0", "https://developer.android.com/jetpack/compose"),
    LicenseItem("Kotlin stdlib", "Apache-2.0", "https://kotlinlang.org"),
)

private fun openUrl(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}

/** 关于页：版本 / GitHub / 隐私说明 / 开源组件声明 */
@Composable
internal fun AboutSettingsPage(controller: BrowserController, onBack: () -> Unit) {
    val context = LocalContext.current
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull().orEmpty()
    }

    val scroll = rememberScrollState()
    SubPageScaffold(stringResource(R.string.entry_about), onBack) { pad ->
        SettingsJumpTarget(scroll, SubPage.About)
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(scroll)
                .settingsList(),
        ) {
            Text(
                stringResource(R.string.app_name),
                Modifier.padding(start = 16.dp, top = 20.dp, end = 16.dp),
                style = MaterialTheme.typography.headlineSmall,
            )
            // 版本号彩蛋：300ms 内连点 6 次解锁隐藏功能（SS 出口）。
            // 每次点击背景短暂高亮，给「按到了」的即时反馈
            var tapCount by remember { mutableIntStateOf(0) }
            var lastTapAt by remember { mutableLongStateOf(0L) }
            var flash by remember { mutableStateOf(false) }
            LaunchedEffect(flash) {
                if (flash) { delay(180); flash = false }
            }
            val flashBg by animateColorAsState(
                targetValue = if (flash) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                else Color.Transparent,
                animationSpec = tween(90),
                label = "versionFlash",
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .background(flashBg, MaterialTheme.shapes.small)
                    .clickable {
                        flash = true
                        val now = System.currentTimeMillis()
                        tapCount = if (now - lastTapAt < 300) tapCount + 1 else 1
                        lastTapAt = now
                        if (tapCount >= 6) {
                            tapCount = 0
                            controller.container.settings.update { it.copy(unlockSs = true) }
                            Toast.makeText(
                                context, context.getString(R.string.easter_egg_unlocked),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    stringResource(R.string.about_version, version),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SettingItem(
                title = stringResource(R.string.about_github),
                value = "github.com/felix021/puff-browser",
                entryId = "about.github",
            ) { openUrl(context, "https://github.com/felix021/puff-browser") }

            SectionHeader(stringResource(R.string.about_privacy_title), entryId = "about.privacy")
            Text(
                stringResource(R.string.about_privacy_body),
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
            )

            SectionHeader(stringResource(R.string.about_licenses_title), entryId = "about.licenses")
            Text(
                stringResource(R.string.about_license_notice),
                Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LICENSES.forEach { lic ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { openUrl(context, lic.url) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(lic.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            lic.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                stringResource(R.string.about_easylist_notice),
                Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}
