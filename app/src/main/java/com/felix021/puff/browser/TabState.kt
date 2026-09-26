package com.felix021.puff.browser

/** 一个标签页的可观察状态（WebView 实例由 TabManager 持有，不进 Compose 状态） */
data class TabState(
    val id: String,
    val url: String,
    val title: String = "",
    val progress: Int = 100,
    val loading: Boolean = false,
    val desktopMode: Boolean = false,
    val error: String? = null,
) {
    val displayTitle: String
        get() = title.ifBlank { url }
}
