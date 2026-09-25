package cn.felix021.navigateur.data

import android.content.Context
import cn.felix021.navigateur.security.CryptoBox
import org.json.JSONObject

/** 站点密码库：按 host 存一条凭据，内容经 Keystore AES-GCM 加密后落盘 */
class PasswordStore(context: Context) {
    private val prefs = context.getSharedPreferences("passwords", Context.MODE_PRIVATE)

    data class Credential(val username: String, val password: String)

    fun get(host: String): Credential? {
        val enc = prefs.getString(host, null) ?: return null
        return try {
            val o = JSONObject(CryptoBox.decrypt(enc))
            Credential(o.getString("u"), o.getString("p"))
        } catch (e: Exception) {
            // Keystore 失效或密文损坏：丢弃该条，避免永久卡死
            prefs.edit().remove(host).apply()
            null
        }
    }

    fun upsert(host: String, username: String, password: String) {
        val plain = JSONObject().put("u", username).put("p", password).toString()
        prefs.edit().putString(host, CryptoBox.encrypt(plain)).apply()
    }

    fun remove(host: String) {
        prefs.edit().remove(host).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun hosts(): List<String> = prefs.all.keys.sorted()
}
