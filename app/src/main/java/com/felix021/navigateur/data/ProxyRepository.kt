package com.felix021.navigateur.data

import android.util.Log
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import java.util.concurrent.Executor
import org.json.JSONArray
import org.json.JSONObject

/** 代理出口：DIRECT 无字段；HTTP/SOCKS5 需要 host:port */
data class ProxyProfile(
    val id: String,
    val name: String,
    /** DIRECT / HTTP / SOCKS5 */
    val type: String,
    val host: String = "",
    val port: Int = 0,
) {
    fun url(): String = when (type) {
        "SOCKS5" -> "socks5://$host:$port"
        else -> "http://$host:$port"
    }
}

/** 自动切换规则：pattern → 动作（direct=直连；proxy=走默认出口，占位保持 SwitchyOmega 心智） */
data class ProxyRule(
    val pattern: String,
    /** direct / proxy */
    val action: String,
)

/** 当前模式：direct / auto / <profileId> */
data class ProxySettings(
    val mode: String = "direct",
    /** auto 模式下未命中直连规则时使用的出口 profile id */
    val autoProfileId: String = "",
    val profiles: List<ProxyProfile> = emptyList(),
    val rules: List<ProxyRule> = emptyList(),
)

/**
 * 规则代理：androidx.webkit ProxyController。
 * 能力边界（上游 API）：单出口 + bypass 直连列表，无 PAC；
 * auto 模式 = 默认走出口代理，命中规则的域名直连。
 */
class ProxyRepository {

    private val ex = Executor { it.run() }

    fun apply(s: ProxySettings) {
        try {
            if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) return
            val pc = ProxyController.getInstance()
            when {
                // 直连：清除覆盖
                s.mode == "direct" || profileOf(s) == null -> pc.clearProxyOverride(ex, Runnable {})
                // 自动：出口代理 + 直连规则的 bypass
                s.mode == "auto" -> {
                    val profile = profileOf(s) ?: return pc.clearProxyOverride(ex, Runnable {})
                    val b = ProxyConfig.Builder()
                        .addProxyRule(profile.url())
                        .bypassSimpleHostnames()
                    b.addBypassRule("<local>")
                    s.rules.filter { it.action == "direct" }.forEach { r ->
                        b.addBypassRule(compileBypass(r.pattern))
                    }
                    pc.setProxyOverride(b.build(), ex, Runnable {})
                }
                // 指定 profile
                else -> {
                    val profile = profileOf(s)!!
                    pc.setProxyOverride(
                        ProxyConfig.Builder()
                            .addProxyRule(profile.url())
                            .bypassSimpleHostnames()
                            .addBypassRule("<local>")
                            .build(),
                        ex, Runnable {},
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("NavigateurProxy", "apply failed", e)
        }
    }

    private fun profileOf(s: ProxySettings): ProxyProfile? =
        s.profiles.firstOrNull { it.id == if (s.mode == "auto") s.autoProfileId else s.mode }

    /** 用户 pattern → Chromium bypass 规则（.example.com 后缀含自身与子域；通配符原样） */
    private fun compileBypass(pattern: String): String {
        val p = pattern.trim().removePrefix(".").lowercase()
        return if (p.contains('*')) pattern else ".$p"
    }

    companion object {
        fun toJson(s: ProxySettings): String = JSONObject().apply {
            put("mode", s.mode)
            put("autoProfileId", s.autoProfileId)
            put("profiles", JSONArray(s.profiles.map {
                JSONObject().put("id", it.id).put("name", it.name)
                    .put("type", it.type).put("host", it.host).put("port", it.port)
            }))
            put("rules", JSONArray(s.rules.map {
                JSONObject().put("pattern", it.pattern).put("action", it.action)
            }))
        }.toString()

        fun fromJson(raw: String?): ProxySettings = runCatching {
            val o = JSONObject(raw ?: return@runCatching ProxySettings())
            ProxySettings(
                mode = o.optString("mode", "direct"),
                autoProfileId = o.optString("autoProfileId", ""),
                profiles = (0 until o.optJSONArray("profiles").length()).map { i ->
                    val p = o.optJSONArray("profiles").getJSONObject(i)
                    ProxyProfile(
                        p.optString("id"), p.optString("name"),
                        p.optString("type", "HTTP"), p.optString("host"), p.optInt("port"),
                    )
                },
                rules = (0 until o.optJSONArray("rules").length()).map { i ->
                    val r = o.optJSONArray("rules").getJSONObject(i)
                    ProxyRule(r.optString("pattern"), r.optString("action", "direct"))
                },
            )
        }.getOrDefault(ProxySettings())
    }
}
