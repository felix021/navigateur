package com.felix021.navigateur

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.felix021.navigateur.ui.AppRoot
import com.felix021.navigateur.ui.BrowserController

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
        // 方便 chrome://inspect 调试页面（个人应用，保持开启）
        WebView.setWebContentsDebuggingEnabled(true)
        controller = BrowserController(this)
        setContent { AppRoot(controller) }
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) controller.tabManager.destroyAll()
    }
}
