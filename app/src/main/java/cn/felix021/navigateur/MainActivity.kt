package cn.felix021.navigateur

import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import cn.felix021.navigateur.ui.AppRoot
import cn.felix021.navigateur.ui.BrowserController

class MainActivity : AppCompatActivity() {

    lateinit var controller: BrowserController
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
