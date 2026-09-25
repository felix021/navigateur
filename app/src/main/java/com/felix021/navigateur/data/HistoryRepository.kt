package com.felix021.navigateur.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class HistoryEntry(val url: String, val title: String, val visitedAt: Long)

/** 浏览历史：去重（同 URL 更新时间），供地址栏输入建议 */
class HistoryRepository(context: Context) {
    private val prefs = context.getSharedPreferences("history", Context.MODE_PRIVATE)

    private val _history = MutableStateFlow(read())
    val history: StateFlow<List<HistoryEntry>> = _history

    fun add(url: String, title: String) {
        if (url.isBlank() || url == "about:blank") return
        val prev = _history.value.firstOrNull { it.url == url }
        val entry = HistoryEntry(url, title.ifBlank { prev?.title.orEmpty() }, System.currentTimeMillis())
        write((_history.value.filterNot { it.url == url } + entry)
            .sortedByDescending { it.visitedAt }
            .take(MAX))
    }

    fun removeByHost(host: String) {
        write(_history.value.filterNot { it.url.contains("://$host/", ignoreCase = true) || it.url.endsWith("://$host", true) })
    }

    fun clear() = write(emptyList())

    /** 地址栏建议：URL 或标题包含查询串 */
    fun suggest(query: String, limit: Int = 6): List<HistoryEntry> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        return _history.value.filter {
            it.url.contains(q, true) || it.title.contains(q, true)
        }.take(limit)
    }

    /** 历史中出现过的域名，供按站点清理 */
    fun hosts(): List<String> = _history.value
        .mapNotNull { runCatching { java.net.URI(it.url).host }.getOrNull() }
        .filter { it.isNotBlank() }
        .distinct()

    private fun write(list: List<HistoryEntry>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("u", it.url).put("t", it.title).put("ts", it.visitedAt))
        }
        prefs.edit().putString("history", arr.toString()).apply()
        _history.value = list
    }

    private fun read(): List<HistoryEntry> = runCatching {
        val arr = JSONArray(prefs.getString("history", "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            HistoryEntry(o.getString("u"), o.optString("t"), o.optLong("ts"))
        }
    }.getOrDefault(emptyList())

    private companion object {
        const val MAX = 500
    }
}
