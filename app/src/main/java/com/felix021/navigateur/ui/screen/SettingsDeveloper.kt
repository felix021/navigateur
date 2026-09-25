package com.felix021.navigateur.ui.screen

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
import com.felix021.navigateur.ui.BrowserController

/** 开发者二级页：远程调试 / 页面内 eruda */
@Composable
internal fun DeveloperSettingsPage(controller: BrowserController, onBack: () -> Unit) {
    val settings by controller.container.settings.settings.collectAsState()

    SubPageScaffold("开发者", onBack) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()),
        ) {
            SwitchItem(
                title = "远程调试（chrome://inspect / CDP）",
                subtitle = "默认关闭。开启后电脑经 adb 可用完整 Chrome DevTools 检查页面；" +
                    "也可用命令行开关：am start --ez remote_debug true",
                checked = settings.remoteDebug,
                onChange = { enabled ->
                    controller.container.settings.update { it.copy(remoteDebug = enabled) }
                },
            )
            SwitchItem(
                title = "页面内开发者工具（eruda）",
                subtitle = "手机上直接查看 Console / Elements / Network / Storage；" +
                    "也可用命令行开关：am start --ez dev_tools true",
                checked = settings.devTools,
                onChange = { enabled ->
                    controller.container.settings.update { it.copy(devTools = enabled) }
                },
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}
