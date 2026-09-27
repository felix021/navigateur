package com.felix021.puff

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.felix021.puff.ui.AppRoot
import com.felix021.puff.ui.BrowserController
import com.felix021.puff.util.NotifPermission

class MainActivity : AppCompatActivity() {

    lateinit var controller: BrowserController
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 允许内容延伸到打孔屏 cutout 区（否则横屏全屏时系统会加白边/黑边 letterbox），
        // 安全区避让由 Compose displayCutout insets 处理
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        controller = BrowserController(this)
        applyDebugIntent(intent)
        setContent { AppRoot(controller) }
    }

    /** 通知权限的系统授权结果转发给 gate（首次下载时按需申请） */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 7001) {
            NotifPermission.handleResult(requestCode, grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED)
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        applyDebugIntent(intent)
    }

    /** adb 调试入口：am start ... --ez remote_debug/--ez dev_tools true|false，免手动进设置切换 */
    private fun applyDebugIntent(i: android.content.Intent?) {
        i ?: return
        if (i.hasExtra("remote_debug")) {
            val on = i.getBooleanExtra("remote_debug", false)
            if (on != controller.container.settings.current.remoteDebug) {
                controller.container.settings.update { it.copy(remoteDebug = on) }
            }
        }
        if (i.hasExtra("dev_tools")) {
            val on = i.getBooleanExtra("dev_tools", false)
            if (on != controller.container.settings.current.devTools) {
                controller.container.settings.update { it.copy(devTools = on) }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 语言切换等配置变化也会重建 Activity（isFinishing=false），旧 WebView 必须释放，
        // 否则持有已销毁 Activity context 泄漏；标签会话由 TabSessionStore 恢复
        if (::controller.isInitialized) controller.tabManager.destroyAll()
    }
}
