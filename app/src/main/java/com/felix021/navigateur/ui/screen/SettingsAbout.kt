package com.felix021.navigateur.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.felix021.navigateur.R
import com.felix021.navigateur.ui.BrowserController

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
            Text(
                stringResource(R.string.about_version, version),
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SettingItem(
                title = stringResource(R.string.about_github),
                value = "github.com/felix021/navigateur",
                entryId = "about.github",
            ) { openUrl(context, "https://github.com/felix021/navigateur") }

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
