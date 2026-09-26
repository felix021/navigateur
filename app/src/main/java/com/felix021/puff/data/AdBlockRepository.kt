package com.felix021.puff.data

import android.content.Context
import com.felix021.puff.browser.AdBlockEngine
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 规则元信息（设置页展示） */
data class AdBlockStatus(
    val ruleCount: Int = 0,
    /** 最近一次成功更新的时间戳（ms），0 = 从未更新（预置） */
    val updatedAt: Long = 0,
    val updating: Boolean = false,
    val lastError: String? = null,
)

/**
 * 广告规则仓库：assets 预置 → filesDir 覆盖更新。
 * 下载源为 EasyList 官方 CDN，两份合并解析后原子替换引擎数据。
 */
class AdBlockRepository(
    context: Context,
    private val engine: AdBlockEngine,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    private val _status = MutableStateFlow(AdBlockStatus())
    val status: StateFlow<AdBlockStatus> = _status

    companion object {
        private const val DIR = "adblock"
        private val SOURCES = listOf(
            "easylist" to "https://easylist-downloads.adblockplus.org/easylist.txt",
            "easylistchina" to "https://easylist-downloads.adblockplus.org/easylistchina.txt",
        )
    }

    /** 启动时加载：filesDir 有就用文件，否则拷 assets */
    fun load() {
        scope.launch {
            val dir = File(appContext.filesDir, DIR)
            var fromAssets = false
            if (SOURCES.all { File(dir, it.first + ".txt").exists() }) {
                loadIntoEngine(SOURCES.map { File(dir, it.first + ".txt").readText() }, updatedAt())
            } else {
                fromAssets = true
                val texts = SOURCES.mapNotNull { (name, _) ->
                    runCatching {
                        appContext.assets.open("$DIR/$name.txt").bufferedReader().readText()
                    }.getOrNull()
                }
                loadIntoEngine(texts, 0)
            }
            android.util.Log.i(
                "PuffAD",
                "rules loaded (${if (fromAssets) "assets" else "files"}) count=${engine.data.ruleCount}",
            )
        }
    }

    private fun updatedAt(): Long =
        appContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getLong("adblock_updated_at", 0)

    private fun loadIntoEngine(texts: List<String>, ts: Long) {
        val data = AdBlockEngine.parse(texts)
        engine.replace(data)
        _status.value = AdBlockStatus(ruleCount = data.ruleCount, updatedAt = ts)
    }

    /** 在线更新规则（手动触发） */
    fun update() {
        if (_status.value.updating) return
        _status.value = _status.value.copy(updating = true, lastError = null)
        scope.launch {
            try {
                val dir = File(appContext.filesDir, DIR).apply { mkdirs() }
                val texts = SOURCES.map { (name, url) ->
                    val text = download(url)
                    File(dir, "$name.txt").writeText(text)
                    text
                }
                appContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
                    .edit().putLong("adblock_updated_at", System.currentTimeMillis()).apply()
                loadIntoEngine(texts, System.currentTimeMillis())
                _status.value = _status.value.copy(updating = false)
            } catch (e: Exception) {
                _status.value = _status.value.copy(updating = false, lastError = e.message)
            }
        }
    }

    private fun download(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 30000
        try {
            if (conn.responseCode !in 200..299) throw IllegalStateException("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader().readText()
        } finally {
            conn.disconnect()
        }
    }
}
