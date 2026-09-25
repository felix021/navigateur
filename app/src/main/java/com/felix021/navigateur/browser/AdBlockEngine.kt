package com.felix021.navigateur.browser

import android.net.Uri

/**
 * 广告拦截引擎：EasyList 子集匹配。
 *
 * 解析范围（覆盖规则量大头，语义明确无歧义）：
 * - `||domain^`（可带 $third-party 等选项）→ 整域拦截（含子域）
 * - `@@||domain^` → 例外放行（选项复杂的跳过）
 * - `domain##selector`（单域名、无 ~ 排除前缀）→ 元素隐藏 CSS
 * 其余（通用 ##、纯关键词、||domain/path）不解析。
 *
 * 数据是不可变快照，更新时整体原子替换，请求热路径无锁。
 */
class AdBlockEngine {

    data class Data(
        val blockedDomains: Set<String> = emptySet(),
        val allowedDomains: Set<String> = emptySet(),
        /** 域名 -> 元素隐藏选择器 */
        val cssByDomain: Map<String, List<String>> = emptyMap(),
        val ruleCount: Int = 0,
    )

    @Volatile
    var data: Data = Data()
        private set

    fun replace(d: Data) {
        data = d
    }

    /** 请求拦截判定：host 命中黑名单域名（逐级父域匹配），例外优先 */
    fun shouldBlock(url: String): Boolean {
        val host = try {
            Uri.parse(url).host
        } catch (e: Exception) {
            null
        } ?: return false
        val d = data
        if (matchesDomain(d.allowedDomains, host)) return false
        return matchesDomain(d.blockedDomains, host)
    }

    /** 当前站点（含子域）命中的元素隐藏选择器 */
    fun hideSelectors(host: String): List<String> {
        if (host.isEmpty()) return emptyList()
        val d = data
        val out = ArrayList<String>()
        var h = host
        while (true) {
            d.cssByDomain[h]?.let { out.addAll(it) }
            val i = h.indexOf('.')
            if (i < 0) break
            h = h.substring(i + 1)
        }
        return out
    }

    /** 生成注入页面的隐藏 CSS（每 50 条一条规则，坏选择器只影响所在块） */
    fun hideCss(host: String): String {
        val sels = hideSelectors(host).filter { sel ->
            // 过滤非标准选择器（:-abp- 伪类、XPath 等），避免整块规则失效
            !sel.contains("-abp-") && !sel.contains("xpath(") && !sel.contains("style(")
        }
        if (sels.isEmpty()) return ""
        return sels.chunked(50).joinToString("") { chunk ->
            chunk.joinToString(",") { it.trim() } + "{display:none!important;visibility:hidden!important;}"
        }
    }

    private fun matchesDomain(set: Set<String>, host: String): Boolean {
        var h = host
        while (true) {
            if (h in set) return true
            val i = h.indexOf('.')
            if (i < 0) return false
            h = h.substring(i + 1)
        }
    }

    companion object {

        /** 解析 EasyList 文本（可多份合并传入） */
        fun parse(texts: List<String>): Data {
            val blocked = HashSet<String>(65536)
            val allowed = HashSet<String>(2048)
            val css = HashMap<String, MutableList<String>>(8192)
            var count = 0
            for (text in texts) {
                for (raw in text.lineSequence()) {
                    val line = raw.trim()
                    if (line.isEmpty() || line.startsWith("!")) continue
                    when {
                        // @@||domain^ 例外（带复杂选项的跳过）
                        line.startsWith("@@||") -> {
                            val body = line.substring(4)
                            if (body.endsWith("^") && !body.contains('/') && !body.contains('$')) {
                                allowed.add(body.removeSuffix("^").lowercase())
                            }
                        }
                        // ||domain^ / ||domain^$options / ||domain → 整域拦；
                        // ||domain/path（路径级）不解析，避免误伤同域正常资源
                        line.startsWith("||") -> {
                            val body = line.substring(2)
                            val caret = body.indexOf('^')
                            val domain = when {
                                caret >= 0 -> {
                                    val after = body.substring(caret + 1)
                                    if (after.isEmpty() || after.startsWith("$")) body.substring(0, caret) else null
                                }
                                body.none { it == '/' || it == '$' } -> body
                                else -> null
                            }
                            if (domain != null && domain.length > 3 && domain.isValidDomain()) {
                                blocked.add(domain.lowercase())
                                count++
                            }
                        }
                        // domain##selector（排除 ~dom、多域、逗号域等复杂形态）
                        line.contains("##") -> {
                            val idx = line.indexOf("##")
                            val dom = line.substring(0, idx)
                            if (dom.isEmpty() || dom.contains('~') || dom.contains(',')) continue
                            val sels = line.substring(idx + 2).split(',').map { it.trim() }
                            for (sel in sels) {
                                // 允许 .class / #id / tag / [attr]；嵌套 #（如 a ##b 的衍生形态）跳过
                                if (sel.isEmpty() || sel.drop(1).contains('#')) continue
                                css.getOrPut(dom.lowercase()) { ArrayList() }.add(sel)
                                count++
                            }
                        }
                    }
                }
            }
            // 例外与黑名单重叠时以例外为准
            blocked.removeAll(allowed)
            return Data(blocked, allowed, css, count)
        }

        private fun String.isValidDomain(): Boolean =
            isNotEmpty() && length <= 253 &&
                first().isLetterOrDigit() && last().isLetterOrDigit() &&
                all { it.isLetterOrDigit() || it == '.' || it == '-' }
    }
}
