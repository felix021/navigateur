package com.felix021.puff.browser

import org.json.JSONObject

/** 注入页面的 JS 脚本（注意：脚本内避免反引号和 $ 符号） */
object JsScripts {

    /** 登录表单捕获：submit 事件 + 回车兜底；单密码框且非 new-password 才上报 */
    val CAPTURE_HOOK = """
        (function () {
          function install(doc) {
            var w = doc.defaultView;
            if (!w || w.__nvHooked) return;
            w.__nvHooked = true;
            function collect(scope) {
              try {
                var pws = scope.querySelectorAll('input[type=password]');
                if (pws.length !== 1) return null;
                var pw = pws[0];
                if (!pw.value || pw.autocomplete === 'new-password') return null;
                var u = scope.querySelector('input[autocomplete=username i], input[type=email], ' +
                  'input[name*=user i], input[name*=mail i], input[name*=login i], input[name*=account i], ' +
                  'input[type=tel], input[type=text]');
                return { username: u ? u.value : '', password: pw.value };
              } catch (e) { return null; }
            }
            function report(scope) {
              try {
                var d = collect(scope);
                if (d && d.password.length >= 1) PuffBridge.onCredentials(JSON.stringify(d));
              } catch (e) {}
            }
            doc.addEventListener('submit', function (e) { report(e.target || doc); }, true);
            doc.addEventListener('keydown', function (e) {
              if (e.key === 'Enter') setTimeout(function () {
                var f = (e.target && e.target.form) ? e.target.form : doc;
                report(f);
              }, 300);
            }, true);
            // fetch/XHR 登录兜底：点击按钮/提交类元素后延时读一次密码框（无 form 的页面也覆盖）
            doc.addEventListener('click', function (e) {
              var t = e.target;
              var b = t && t.closest ? t.closest('button, input[type=submit], input[type=button], [role=button]') : null;
              if (b) setTimeout(function () {
                var f = b.closest ? (b.closest('form') || doc) : doc;
                report(f);
              }, 500);
            }, true);
          }
          function hookFrames(doc) {
            try {
              var list = doc.querySelectorAll('iframe');
              for (var i = 0; i < list.length; i++) {
                try { install(list[i].contentDocument); } catch (e) {}
              }
            } catch (e) {}
          }
          install(document);
          hookFrames(document);
          // iframe 后加载：延时再挂一次（幂等）
          setTimeout(function () { hookFrames(document); }, 1200);
        })();
    """.trimIndent()

    /**
     * 下载链接拦截：新版 WebView（15x）不再回调 onDownloadStart，
     * 页面层直接捕获常见下载链接交原生处理；扩展名或 download 属性判定。
     */
    val DOWNLOAD_INTERCEPT = """
        (function () {
          if (window.__nvDlHooked) return;
          window.__nvDlHooked = true;
          var EXT = /\.(zip|rar|7z|apk|exe|dmg|msi|pdf|tar|gz|tgz|bz2|xz|iso|mp3|flac|wav|mp4|avi|mkv|mov|epub|mobi|torrent|doc|docx|xls|xlsx|ppt|pptx|csv)$/i;
          document.addEventListener('click', function (e) {
            try {
              var t = e.target;
              var a = t && t.closest ? t.closest('a[href]') : null;
              if (!a) return;
              var url = a.href || '';
              if (url.indexOf('http://') !== 0 && url.indexOf('https://') !== 0) return;
              if (!a.hasAttribute('download') && !EXT.test(a.pathname || '')) return;
              e.preventDefault();
              e.stopPropagation();
              PuffBridge.download(url);
            } catch (err) {}
          }, true);
        })();
    """.trimIndent()

    /**
     * 桌面模式：强制宽视口，避免响应式站点按手机宽度出移动布局。
     * 不能带 initial-scale=1：那会锁死 1:1 渲染，屏幕只能看到页面左上角
     * （eruda 等 fixed/absolute 于 layout 右下的元素直接不可见）；
     * 只写 width 时 WebView 按 loadWithOverviewMode 整页 fit 到屏幕。
     */
    val DESKTOP_VIEWPORT = """
        (function () {
          var m = document.querySelector('meta[name=viewport]');
          if (!m) {
            m = document.createElement('meta');
            m.setAttribute('name', 'viewport');
            (document.head || document.documentElement).appendChild(m);
          }
          m.setAttribute('content', 'width=1280');
        })();
    """.trimIndent()

    /** 页面整体缩放（CSS zoom，含布局与图片），100 时移除 */
    fun zoomCss(percent: Int): String {
        val p = percent.coerceIn(50, 200)
        val value = if (p == 100) "''" else "'" + (p / 100.0) + "'"
        return """
            (function () {
              document.documentElement.style.zoom = $value;
            })();
        """.trimIndent()
    }

    /** 替换 id=<elementId> 的样式元素，空 css 即清除（页面级持久样式注入的通用形态） */
    fun styleCss(elementId: String, css: String): String =
        "var css = " + JSONObject.quote(css) + ";" + """
            (function () {
              var el = document.getElementById(${JSONObject.quote(elementId)});
              if (!css) {
                if (el) el.parentNode.removeChild(el);
                return;
              }
              if (!el) { el = document.createElement('style'); el.id = ${JSONObject.quote(elementId)}; document.head.appendChild(el); }
              el.textContent = css;
            })();
        """.trimIndent()

    /** 字体覆盖：替换 id=nv-font 的样式元素，空 family 即清除覆盖 */
    fun fontCss(family: String): String {
        val fam = family.replace(Regex("[^a-zA-Z0-9 ,\\-']"), "").trim()
        val rule = if (fam.isEmpty()) ""
        else "body,input,textarea,select,button{font-family:$fam !important;}"
        return styleCss("nv-font", rule)
    }

    /** 自动填充：走原生 value setter + 派发 input/change 事件，兼容 React/Vue 受控输入 */
    fun autofill(username: String, password: String): String {
        val payload = JSONObject().put("u", username).put("p", password).toString()
        return """
            (function () {
              var d;
              try { d = JSON.parse(${JSONObject.quote(payload)}); } catch (e) { return; }
              var proto = window.HTMLInputElement && window.HTMLInputElement.prototype;
              if (!proto) return;
              var desc = Object.getOwnPropertyDescriptor(proto, 'value');
              if (!desc || !desc.set) return;
              var pws = document.querySelectorAll('input[type=password]');
              if (!pws.length) return;
              function setVal(el, v) {
                desc.set.call(el, v);
                try {
                  el.dispatchEvent(new Event('input', { bubbles: true }));
                  el.dispatchEvent(new Event('change', { bubbles: true }));
                } catch (e) {}
              }
              if (d.u) {
                var u = document.querySelector(
                  'input[autocomplete=username i], input[type=email], input[type=tel], input[type=text]');
                if (u) setVal(u, d.u);
              }
              if (d.p) setVal(pws[0], d.p);
            })();
        """.trimIndent()
    }
}
