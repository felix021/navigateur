package cn.felix021.navigateur.browser

import org.json.JSONObject

/** 注入页面的 JS 脚本（注意：脚本内避免反引号和 $ 符号） */
object JsScripts {

    /** 登录表单捕获：submit 事件 + 回车兜底；单密码框且非 new-password 才上报 */
    val CAPTURE_HOOK = """
        (function () {
          if (window.__nvHooked) return; window.__nvHooked = true;
          function collect(f) {
            try {
              var pws = f.querySelectorAll('input[type=password]');
              if (pws.length !== 1) return null;
              var pw = pws[0];
              if (!pw.value || pw.autocomplete === 'new-password') return null;
              var u = f.querySelector('input[autocomplete=username i], input[type=email], ' +
                'input[name*=user i], input[name*=mail i], input[name*=login i], input[name*=account i], ' +
                'input[type=tel], input[type=text]');
              return { username: u ? u.value : '', password: pw.value };
            } catch (e) { return null; }
          }
          function report(f) {
            try {
              var d = collect(f);
              if (d && d.password.length >= 1) NavigateurBridge.onCredentials(JSON.stringify(d));
            } catch (e) {}
          }
          document.addEventListener('submit', function (e) { report(e.target); }, true);
          document.addEventListener('keydown', function (e) {
            if (e.key === 'Enter' && e.target && e.target.form)
              setTimeout(function () { report(e.target.form); }, 300);
          }, true);
        })();
    """.trimIndent()

    /** 字体覆盖：替换 id=nv-font 的样式元素，空 family 即清除覆盖 */
    fun fontCss(family: String): String {
        val fam = family.replace(Regex("[^a-zA-Z0-9 ,\\-']"), "").trim()
        val rule = if (fam.isEmpty()) ""
        else "body,input,textarea,select,button{font-family:$fam !important;}"
        return """
            (function () {
              var el = document.getElementById('nv-font');
              if (!el) { el = document.createElement('style'); el.id = 'nv-font'; document.head.appendChild(el); }
              el.textContent = ${JSONObject.quote(rule)};
            })();
        """.trimIndent()
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
