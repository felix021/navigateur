package com.felix021.puff.data

import android.util.Log
import android.util.Base64
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import java.util.concurrent.Executor
import org.json.JSONArray
import org.json.JSONObject

/** 代理出口：DIRECT 无字段；HTTP/SOCKS5 需要 host:port；SS 另需 method/password */
data class ProxyProfile(
    val id: String,
    val name: String,
    /** DIRECT / HTTP / SOCKS5 / SS */
    val type: String,
    val host: String = "",
    val port: Int = 0,
    /** SS 加密方式（见 SsMethods.SUPPORTED） */
    val method: String = "",
    /** SS 密码（明文存本地 settings，与其它设置同权限域） */
    val password: String = "",
) {
    /** 直连型出口的 URL；SS 走本地隧道（由 ProxyRepository 换成 127.0.0.1 端口） */
    fun url(): String = when (type) {
        "SOCKS5" -> "socks5://$host:$port"
        "SS" -> "socks5://127.0.0.1:0" // 占位，实际由 ProxyRepository 注入隧道端口
        else -> "http://$host:$port"
    }
}

/** 自动切换规则：pattern → 动作（direct=直连；proxy=走默认出口） */
data class ProxyRule(
    val pattern: String,
    /** direct / proxy */
    val action: String,
)

/** 当前模式：direct / auto / <profileId> */
data class ProxySettings(
    val mode: String = "direct",
    /** auto 模式下未命中规则时使用的出口 profile id */
    val autoProfileId: String = "",
    val profiles: List<ProxyProfile> = emptyList(),
    val rules: List<ProxyRule> = emptyList(),
    /**
     * auto 模式未命中任何规则时的走向：
     * proxy = 默认走代理，命中 direct 规则的域名直连（SwitchyOmega 风格）
     * direct = 默认直连，命中 proxy 规则的域名走代理（AutoProxy / GFW list 语义）
     */
    val autoDefault: String = "proxy",
)

/** 规则列表解析结果（导入反馈用）：rules=有效规则，skipped=跳过的行数 */
data class ProxyRuleParseResult(val rules: List<ProxyRule>, val skipped: Int)

/**
 * 规则代理：androidx.webkit ProxyController。
 * 能力边界（上游 API）：单出口 + bypass 直连列表，无 PAC。
 *
 * - autoDefault=proxy：静态配置（默认出口 + direct 规则转 bypass），逐请求生效
 * - autoDefault=direct：ProxyConfig 表达不了「只有这些走代理」，只能按当前主文档
 *   host 是否命中 proxy 规则动态开关整个 override；同页面的子资源跟随主文档走向
 */
class ProxyRepository {

    private val ex = Executor { it.run() }

    /** SS 出口的本地隧道（WebView 只认 SOCKS5，ss 流量在进程内中转） */
    private val tunnel = SsTunnel()

    @Volatile
    private var settings = ProxySettings()

    /** 当前主文档 host（主线程更新），autoDefault=direct 时决定开关 */
    @Volatile
    private var host = ""

    /** 设置变化（AppContainer 收集） */
    fun apply(s: ProxySettings) {
        settings = s
        reapply()
    }

    /** 主文档 host 变化（TabManager.refreshCurrentHost 调） */
    fun onHostChanged(h: String) {
        if (h == host) return
        host = h
        reapply()
    }

    private fun reapply() {
        val s = settings
        val h = host
        try {
            if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) return
            val pc = ProxyController.getInstance()
            val profile = profileOf(s)
            val proxyUrl = profile?.let { resolveUrl(it) }
            if (profile == null || proxyUrl == null) tunnel.stop()
            when {
                // 直连 / SS 隧道启动失败：清除覆盖
                s.mode == "direct" || proxyUrl == null -> pc.clearProxyOverride(ex, Runnable {})
                // 指定 profile：静态全量代理
                s.mode != "auto" -> pc.setProxyOverride(
                    ProxyConfig.Builder()
                        .addProxyRule(proxyUrl)
                        .bypassSimpleHostnames()
                        .addBypassRule("<local>")
                        .build(),
                    ex, Runnable {},
                )
                s.autoDefault == "direct" -> {
                    // AutoProxy 语义：按主文档命中决定开关
                    if (useProxyFor(s, h)) {
                        pc.setProxyOverride(autoConfig(s, proxyUrl), ex, Runnable {})
                    } else {
                        pc.clearProxyOverride(ex, Runnable {})
                    }
                }
                // SwitchyOmega 语义：静态默认走出口 + direct 规则 bypass
                else -> pc.setProxyOverride(autoConfig(s, proxyUrl), ex, Runnable {})
            }
        } catch (e: Exception) {
            Log.w("PuffProxy", "apply failed", e)
        }
    }

    /** autoDefault=direct 时：direct 规则优先，其次 proxy 规则，最后直连 */
    private fun useProxyFor(s: ProxySettings, h: String): Boolean {
        if (h.isEmpty()) return false
        if (s.rules.any { it.action == "direct" && matchHost(h, it.pattern) }) return false
        return s.rules.any { it.action == "proxy" && matchHost(h, it.pattern) }
    }

    private fun resolveUrl(profile: ProxyProfile): String? = when {
        profile.type != "SS" -> profile.url()
        else -> {
            val port = tunnel.ensure(profile)
            if (port > 0) "socks5://127.0.0.1:$port" else null
        }
    }

    private fun autoConfig(s: ProxySettings, proxyUrl: String): ProxyConfig {
        val b = ProxyConfig.Builder()
            .addProxyRule(proxyUrl)
            .bypassSimpleHostnames()
        b.addBypassRule("<local>")
        s.rules.filter { it.action == "direct" }.forEach { r ->
            b.addBypassRule(compileBypass(r.pattern))
        }
        return b.build()
    }

    private fun profileOf(s: ProxySettings): ProxyProfile? =
        s.profiles.firstOrNull { it.id == if (s.mode == "auto") s.autoProfileId else s.mode }

    /** host 是否命中 pattern：支持 example.com / .example.com / *.example.com（后缀含自身与子域） */
    private fun matchHost(host: String, pattern: String): Boolean {
        val p = pattern.trim().removePrefix("*.").removePrefix(".").lowercase()
        if (p.isEmpty() || host.isEmpty()) return false
        return host == p || host.endsWith(".$p")
    }

    /** 用户 pattern → Chromium bypass 规则（.example.com 后缀含自身与子域；通配符原样） */
    private fun compileBypass(pattern: String): String {
        val p = pattern.trim().removePrefix(".").lowercase()
        return if (p.contains('*')) pattern else ".$p"
    }

    companion object {
        fun toJson(s: ProxySettings): String = JSONObject().apply {
            put("mode", s.mode)
            put("autoProfileId", s.autoProfileId)
            put("autoDefault", s.autoDefault)
            put("profiles", JSONArray(s.profiles.map {
                JSONObject().put("id", it.id).put("name", it.name)
                    .put("type", it.type).put("host", it.host).put("port", it.port)
                    .put("method", it.method).put("password", it.password)
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
                autoDefault = o.optString("autoDefault", "proxy"),
                profiles = (0 until o.optJSONArray("profiles").length()).map { i ->
                    val p = o.optJSONArray("profiles").getJSONObject(i)
                    ProxyProfile(
                        p.optString("id"), p.optString("name"),
                        p.optString("type", "HTTP"), p.optString("host"), p.optInt("port"),
                        p.optString("method"), p.optString("password"),
                    )
                },
                rules = (0 until o.optJSONArray("rules").length()).map { i ->
                    val r = o.optJSONArray("rules").getJSONObject(i)
                    ProxyRule(r.optString("pattern"), r.optString("action", "direct"))
                },
            )
        }.getOrDefault(ProxySettings())

        /**
         * 解析 ss:// 链接（SIP002 + 旧整段 base64 格式），不支持 plugin 参数。
         * 成功返回带 name/host/port/method/password 的 SS 出口（id 由调用方补）。
         */
        fun parseSsLink(raw: String): ProxyProfile? = runCatching {
            var s = raw.trim()
            require(s.startsWith("ss://"))
            s = s.removePrefix("ss://")
            val tag = s.indexOf('#').let { i ->
                if (i >= 0) java.net.URLDecoder.decode(s.substring(i + 1), "UTF-8") else ""
            }.also { s = if (s.contains('#')) s.substring(0, s.indexOf('#')) else s }

            val userInfo: String
            val hostPort: String
            if (s.contains('@')) { // SIP002：userinfo@host:port
                val at = s.indexOf('@')
                val ui = s.substring(0, at)
                hostPort = s.substring(at + 1)
                // userinfo 可能是裸 method:password 或其 base64
                userInfo = if (ui.contains(':')) ui
                else String(Base64.decode(ui, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP), Charsets.UTF_8)
            } else { // 旧格式：整段 base64(method:password@host:port)
                val decoded = String(Base64.decode(s, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP), Charsets.UTF_8)
                val at = decoded.indexOf('@')
                require(at > 0)
                userInfo = decoded.substring(0, at)
                hostPort = decoded.substring(at + 1)
            }
            val sep = userInfo.indexOf(':')
            require(sep > 0)
            val method = userInfo.substring(0, sep)
            val password = userInfo.substring(sep + 1)
            // host:port（v2 插件参数 ;... 剥掉）
            val hp = hostPort.substringBefore(';')
            val hpSep = hp.lastIndexOf(':')
            require(hpSep > 0)
            val port = hp.substring(hpSep + 1).toInt()
            require(port in 1..65535)
            val host = hp.substring(0, hpSep).trim('[', ']')
            ProxyProfile(
                id = java.util.UUID.randomUUID().toString(),
                name = tag.ifBlank { host },
                type = "SS",
                host = host,
                port = port,
                method = method,
                password = password,
            )
        }.getOrNull()

        /**
         * 解析 AutoProxy / ABP 风格规则列表（gfwlist 即此格式）：
         * - `||example.com^`      → 走代理
         * - `@@||example.com^`    → 例外 = 直连
         * - `example.com`         → 走代理（裸域名行）
         * - `!` 注释、`[AutoProxy x]` 头、带路径/正则/选项的规则 → 跳过
         * gfwlist 官方仓库整个文件是 base64，自动尝试解码。
         */
        fun parseRules(raw: String): ProxyRuleParseResult {
            var text = raw.trim()
            // 无任何可识别规则行且像 base64 → 尝试解码（gfwlist 特征）
            if (!text.contains("||") && !text.contains("\n")) {
                runCatching {
                    val decoded = String(Base64.decode(text, Base64.DEFAULT), Charsets.UTF_8)
                    if (decoded.contains("||")) text = decoded
                }
            }
            val out = LinkedHashMap<String, ProxyRule>() // pattern 去重，先到先得
            var skipped = 0
            for (line in text.lineSequence()) {
                val t = line.trim()
                if (t.isEmpty() || t.startsWith("!") || t.startsWith("[")) continue
                val exception = t.startsWith("@@")
                val body = if (exception) t.substring(2) else t
                // 只收纯域名规则：||domain^ 可带 $options（选项对域名级引擎无意义，剥掉）
                val m = Regex("""^\|\|([A-Za-z0-9.\-]+)\^?(?:\$[^/]*)?$""").matchEntire(body)
                if (m != null) {
                    val domain = m.groupValues[1].lowercase()
                    val action = if (exception) "direct" else "proxy"
                    out.putIfAbsent(domain, ProxyRule(domain, action))
                    continue
                }
                // 裸域名行（部分列表用）
                if (Regex("""^[A-Za-z0-9.\-]+\.[A-Za-z]{2,}$""").matches(t)) {
                    out.putIfAbsent(t.lowercase(), ProxyRule(t.lowercase(), "proxy"))
                    continue
                }
                skipped++
            }
            return ProxyRuleParseResult(out.values.toList(), skipped)
        }
    }
}
