package com.felix021.navigateur.data

/** UA 预设：ua 为 null 表示"默认（本机，去 wv 标记）" */
data class UaPreset(
    val id: String,
    val label: String,
    val ua: String?,
    val isDesktop: Boolean = false,
)

object UaPresets {
    const val CUSTOM = "custom"

    /** 菜单"桌面模式"在预设为默认时使用的桌面 UA（macOS Chrome） */
    const val DESKTOP_MAC_UA =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"

    val ALL = listOf(
        UaPreset("default", "默认（本机）", null),
        UaPreset("win", "桌面 · Windows", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36", true),
        UaPreset("mac", "桌面 · macOS", DESKTOP_MAC_UA, true),
        UaPreset("linux", "桌面 · Linux", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36", true),
        UaPreset("iphone", "iPhone", "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1", false),
        UaPreset("huawei", "华为 Mate 60 Pro", "Mozilla/5.0 (Linux; Android 14; HarmonyOS; ALN-AL00) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36", false),
        UaPreset("xiaomi", "小米 14", "Mozilla/5.0 (Linux; Android 14; 23127PN0CC) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36", false),
        UaPreset("vivo", "vivo X100", "Mozilla/5.0 (Linux; Android 14; V2310A) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36", false),
        UaPreset("oppo", "OPPO Find X7", "Mozilla/5.0 (Linux; Android 14; PHZ110) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36", false),
        UaPreset(CUSTOM, "自定义…", "", false),
    )

    fun byId(id: String): UaPreset = ALL.firstOrNull { it.id == id } ?: ALL[0]
}
