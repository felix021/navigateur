package com.felix021.puff.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** 打开标签的持久化（重启恢复，URL 级） */
class TabSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("tab_session", Context.MODE_PRIVATE)

    data class SavedTab(val url: String, val title: String, val desktopMode: Boolean)
    data class Session(val tabs: List<SavedTab>, val selectedIndex: Int)

    fun save(tabs: List<SavedTab>, selectedIndex: Int) {
        val arr = JSONArray()
        tabs.take(MAX).forEach {
            arr.put(
                JSONObject()
                    .put("url", it.url)
                    .put("title", it.title)
                    .put("desktop", it.desktopMode)
            )
        }
        prefs.edit()
            .putString("tabs", arr.toString())
            .putInt("idx", selectedIndex.coerceIn(0, (tabs.size - 1).coerceAtLeast(0)))
            .apply()
    }

    fun restore(): Session? = runCatching {
        val raw = prefs.getString("tabs", null) ?: return@runCatching null
        val arr = JSONArray(raw)
        if (arr.length() == 0) return@runCatching null
        val tabs = (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            SavedTab(o.getString("url"), o.optString("title"), o.optBoolean("desktop"))
        }
        Session(tabs, prefs.getInt("idx", 0).coerceIn(0, tabs.size - 1))
    }.getOrNull()

    private companion object {
        const val MAX = 20
    }
}
