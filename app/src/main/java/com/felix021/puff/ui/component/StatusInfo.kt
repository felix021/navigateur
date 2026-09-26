package com.felix021.puff.ui.component

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.res.stringResource
import com.felix021.puff.R

data class StatusInfo(
    val time: String,
    val batteryPercent: Int?,
    val charging: Boolean,
    val wifi: Boolean,
    val cellular: Boolean,
)

/** 读取时间/电量/网络（约 20 秒刷新一次，个人用足够） */
@Composable
fun rememberStatusInfo(): StatusInfo {
    val context = LocalContext.current
    var info by remember { mutableStateOf(readStatus(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(20_000)
            info = readStatus(context)
        }
    }
    return info
}

private fun readStatus(context: Context): StatusInfo {
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    val battery = runCatching {
        val bm = context.getSystemService(BatteryManager::class.java)
        bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it in 0..100 }
    }.getOrNull()
    val charging = runCatching {
        val i = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }.getOrDefault(false)
    val caps = runCatching {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        cm?.getNetworkCapabilities(cm.activeNetwork)
    }.getOrNull()
    return StatusInfo(
        time = time,
        batteryPercent = battery,
        charging = charging,
        wifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true,
        cellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true,
    )
}

/** 竖排状态条：放在横屏 cutout 安全区竖带里（时间/电量/网络）。
 *  背景/前景色由调用方按页面边缘取色传入；内容靠下排布避开中部的打孔 */
@Composable
fun StatusStripVertical(
    modifier: Modifier = Modifier,
    bgColor: Color = Color(0xF0101014),
    fgColor: Color = Color.White,
) {
    val info = rememberStatusInfo()
    val timeParts = info.time.split(":")
    Column(
        modifier
            .background(bgColor)
            .padding(bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text(
            timeParts.getOrNull(0).orEmpty(),
            style = MaterialTheme.typography.titleSmall,
            color = fgColor,
        )
        Text(
            timeParts.getOrNull(1).orEmpty(),
            style = MaterialTheme.typography.titleSmall,
            color = fgColor,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            buildString {
                if (info.charging) append("⚡")
                append(info.batteryPercent?.toString() ?: "–")
                append('%')
            },
            style = MaterialTheme.typography.labelSmall,
            color = fgColor,
        )
        Spacer(Modifier.height(8.dp))
        Icon(
            when {
                info.wifi -> Icons.Filled.Wifi
                info.cellular -> Icons.Filled.SignalCellular4Bar
                else -> Icons.Filled.CloudOff
            },
            contentDescription = stringResource(R.string.status_network),
            tint = fgColor,
            modifier = Modifier.size(14.dp),
        )
    }
}

/** 横排半透明悬浮条：无 cutout 设备横屏全屏时的 fallback */
@Composable
fun StatusStripTop(
    modifier: Modifier = Modifier,
    bgColor: Color = Color(0xD0101014),
    fgColor: Color = Color.White,
) {
    val info = rememberStatusInfo()
    Row(
        modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 14.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(info.time, style = MaterialTheme.typography.labelSmall, color = fgColor)
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                when {
                    info.wifi -> Icons.Filled.Wifi
                    info.cellular -> Icons.Filled.SignalCellular4Bar
                    else -> Icons.Filled.CloudOff
                },
                contentDescription = stringResource(R.string.status_network),
                tint = fgColor,
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.width(6.dp))
            if (info.charging) {
                Icon(
                    Icons.Filled.Bolt,
                    contentDescription = stringResource(R.string.status_charging),
                    tint = fgColor,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(2.dp))
            }
            Text(
                "${info.batteryPercent ?: "–"}%",
                style = MaterialTheme.typography.labelSmall,
                color = fgColor,
            )
        }
    }
}
