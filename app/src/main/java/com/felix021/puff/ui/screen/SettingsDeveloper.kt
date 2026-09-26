package com.felix021.puff.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.felix021.puff.ui.BrowserController
import androidx.compose.ui.res.stringResource
import com.felix021.puff.R

/** 开发者二级页：远程调试 / 页面内 eruda */
@Composable
internal fun DeveloperSettingsPage(controller: BrowserController, onBack: () -> Unit) {
    val settings by controller.container.settings.settings.collectAsState()

    val scroll = rememberScrollState()
    SubPageScaffold(stringResource(R.string.entry_developer), onBack) { pad ->
        SettingsJumpTarget(scroll, SubPage.Developer)
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(scroll).settingsList(),
        ) {
            SwitchItem(
                entryId = "d.remote",
                title = stringResource(R.string.dev_remote_title),
                subtitle = stringResource(R.string.dev_remote_desc),
                checked = settings.remoteDebug,
                onChange = { enabled ->
                    controller.container.settings.update { it.copy(remoteDebug = enabled) }
                },
            )
            SwitchItem(
                entryId = "d.eruda",
                title = stringResource(R.string.dev_eruda_title),
                subtitle = stringResource(R.string.dev_eruda_desc),
                checked = settings.devTools,
                onChange = { enabled ->
                    controller.container.settings.update { it.copy(devTools = enabled) }
                },
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}
