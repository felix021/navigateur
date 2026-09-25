package cn.felix021.navigateur.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class Bookmark(
    val id: String,
    val title: String,
    val url: String,
    val createdAt: Long = System.currentTimeMillis(),
)

class BookmarkRepository(context: Context) {
    private val prefs = context.getSharedPreferences("bookmarks", Context.MODE_PRIVATE)

    private val _bookmarks = MutableStateFlow(read())
    val bookmarks: StateFlow<List<Bookmark>> = _bookmarks

    fun isBookmarked(url: String): Boolean = _bookmarks.value.any { it.url == url }

    /** 同 URL 视为更新（重新置顶） */
    fun add(title: String, url: String) {
        val trimmedUrl = url.trim()
        if (trimmedUrl.isEmpty()) return
        val list = _bookmarks.value.filter { it.url != trimmedUrl } +
            Bookmark(
                id = UUID.randomUUID().toString(),
                title = title.trim().ifEmpty { trimmedUrl },
                url = trimmedUrl,
            )
        write(list)
    }

    fun update(id: String, title: String, url: String) {
        val list = _bookmarks.value.map {
            if (it.id == id) it.copy(
                title = title.trim().ifEmpty { url.trim() },
                url = url.trim().ifEmpty { it.url },
            ) else it
        }
        write(list)
    }

    fun remove(id: String) = write(_bookmarks.value.filterNot { it.id == id })

    fun removeByUrl(url: String) = write(_bookmarks.value.filterNot { it.url == url })

    private fun write(list: List<Bookmark>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("id", it.id).put("title", it.title).put("url", it.url).put("ts", it.createdAt))
        }
        prefs.edit().putString("bookmarks", arr.toString()).apply()
        _bookmarks.value = list
    }

    private fun read(): List<Bookmark> = runCatching {
        val arr = JSONArray(prefs.getString("bookmarks", "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Bookmark(o.getString("id"), o.optString("title"), o.getString("url"), o.optLong("ts"))
        }
    }.getOrDefault(emptyList())
}
