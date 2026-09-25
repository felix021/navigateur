package cn.felix021.navigateur.data

import java.net.URLEncoder

data class SearchEngine(val id: String, val name: String, val template: String)

object SearchEngines {
    val ALL = listOf(
        SearchEngine("duckduckgo", "DuckDuckGo", "https://duckduckgo.com/?q=%s"),
        SearchEngine("google", "Google", "https://www.google.com/search?q=%s"),
        SearchEngine("bing", "Bing", "https://www.bing.com/search?q=%s"),
        SearchEngine("baidu", "百度", "https://www.baidu.com/s?wd=%s"),
    )

    fun byId(id: String): SearchEngine = ALL.firstOrNull { it.id == id } ?: ALL[0]

    fun urlFor(id: String, query: String): String =
        byId(id).template.replace("%s", URLEncoder.encode(query, "UTF-8"))
}
