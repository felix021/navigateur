package cn.felix021.navigateur.browser

import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.appcompat.app.AlertDialog

class BrowserChromeClient(
    private val tm: TabManager,
    private val tabId: String,
) : WebChromeClient() {

    override fun onConsoleMessage(message: ConsoleMessage?): Boolean {
        Log.i("NavigateurJS", "[${message?.sourceId()}:${message?.lineNumber()}] ${message?.message()}")
        return false
    }

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        tm.updateState(tabId) { it.copy(progress = newProgress) }
    }

    override fun onReceivedTitle(view: WebView, title: String?) {
        val t = title.orEmpty()
        tm.updateState(tabId) { s -> s.copy(title = t.ifBlank { s.url }) }
    }

    override fun onJsAlert(view: WebView, url: String, message: String?, result: JsResult): Boolean {
        AlertDialog.Builder(tm.context)
            .setMessage(message.orEmpty())
            .setPositiveButton("确定") { _, _ -> result.confirm() }
            .setOnCancelListener { result.cancel() }
            .show()
        return true
    }

    override fun onJsConfirm(view: WebView, url: String, message: String?, result: JsResult): Boolean {
        AlertDialog.Builder(tm.context)
            .setMessage(message.orEmpty())
            .setPositiveButton("确定") { _, _ -> result.confirm() }
            .setNegativeButton("取消") { _, _ -> result.cancel() }
            .setOnCancelListener { result.cancel() }
            .show()
        return true
    }
}
