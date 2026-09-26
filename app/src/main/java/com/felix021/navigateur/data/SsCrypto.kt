package com.felix021.navigateur.data

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Shadowsocks AEAD 加密原语（aes-128/256-gcm、chacha20-ietf-poly1305）。
 *
 * 协议要点（对应 shadowsocks-libev AEAD 模式）：
 * - 密钥 = EVP_BytesToKey(password, MD5 链)，取 cipher key 长度
 * - 每连接每方向独立：随机 salt（= key 长度）→ HKDF-SHA1(info="ss-subkey") 派生 subkey
 * - 每块密文 = [2B 长度][密文][16B tag]，nonce 12 字节大端计数、逐块 +1
 * - 头部块（目标地址+端口）与数据块同一密钥流，TCP 流上先 header 后 data
 *
 * 一个实例绑定「一个方向」（发送或接收），两方向各建一对；
 * subkey 在 newSalt()/setSalt() 后才可用。
 */
class SsCipher(private val method: String, private val masterKey: ByteArray) {

    private val keyLen: Int = SsMethods.keyLen(method)
        ?: throw IllegalArgumentException("unsupported ss method: $method")
    private var subkey: ByteArray = ByteArray(0)
    private val nonce = ByteArray(12)

    /** 出站：生成 salt（返回值即待写入流的字节），并切好本方向 subkey */
    fun newSalt(): ByteArray {
        val salt = ByteArray(keyLen)
        SecureRandom().nextBytes(salt)
        subkey = hkdfSha1(masterKey, salt, keyLen)
        return salt
    }

    /** 入站：从流上读到的 salt */
    fun setSalt(salt: ByteArray) {
        require(salt.size == keyLen) { "bad salt size ${salt.size} != $keyLen" }
        subkey = hkdfSha1(masterKey, salt, keyLen)
    }

    /** 加密一块：返回 [2B 长度][密文+tag] */
    fun seal(plain: ByteArray): ByteArray {
        val out = when (method) {
            "aes-128-gcm", "aes-256-gcm" -> sealGcm(plain)
            else -> sealChaCha(plain)
        }
        bumpNonce()
        return byteArrayOf((out.size shr 8).toByte(), out.size.toByte()) + out
    }

    /** 解密一块（入参为 [密文+tag]，长度前缀已由调用方剥掉） */
    fun open(block: ByteArray): ByteArray {
        require(block.size >= 16) { "short aead block: ${block.size}" }
        val out = when (method) {
            "aes-128-gcm", "aes-256-gcm" -> openGcm(block)
            else -> openChaCha(block)
        }
        bumpNonce()
        return out
    }

    private fun sealGcm(plain: ByteArray): ByteArray {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(subkey, "AES"), GCMParameterSpec(128, nonce))
        return c.doFinal(plain)
    }

    private fun openGcm(block: ByteArray): ByteArray {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, SecretKeySpec(subkey, "AES"), GCMParameterSpec(128, nonce))
        return c.doFinal(block)
    }

    private fun sealChaCha(plain: ByteArray): ByteArray {
        val c = Cipher.getInstance("ChaCha20-Poly1305")
        c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(subkey, "ChaCha20"), IvParameterSpec(nonce))
        return c.doFinal(plain)
    }

    private fun openChaCha(block: ByteArray): ByteArray {
        val c = Cipher.getInstance("ChaCha20-Poly1305")
        c.init(Cipher.DECRYPT_MODE, SecretKeySpec(subkey, "ChaCha20"), IvParameterSpec(nonce))
        return c.doFinal(block)
    }

    private fun bumpNonce() {
        for (i in nonce.indices.reversed()) {
            val v = (nonce[i].toInt() and 0xFF) + 1
            nonce[i] = v.toByte()
            if (v != 0x100) break
        }
    }

    companion object {
        /** 某方法的 master key（EVP_BytesToKey）；不支持的方法返回 null */
        fun masterKeyOf(method: String, password: ByteArray): ByteArray? {
            val keyLen = SsMethods.keyLen(method) ?: return null
            val out = ArrayList<Byte>(32)
            var prev = ByteArray(0)
            while (out.size < keyLen) {
                val md = MessageDigest.getInstance("MD5")
                md.update(prev)
                md.update(password)
                prev = md.digest()
                out.addAll(prev.toList())
            }
            return out.take(keyLen).toByteArray()
        }

        /**
         * HKDF-SHA1（extract + expand）。
         * PRK = HMAC(salt, ikm)；T(i) = HMAC(PRK, T(i-1) || info || i)，
         * OKM = T(1)||T(2)||… 取前 len 字节。SHA1 输出 20B，len=32 必须两块——
         * 单块 copyOf 补零会得到与对端不同的 key（ss 首连即握手失败）。
         */
        fun hkdfSha1(ikm: ByteArray, salt: ByteArray, len: Int): ByteArray {
            val mac = Mac.getInstance("HmacSHA1")
            mac.init(SecretKeySpec(salt, "HmacSHA1"))
            val prk = mac.doFinal(ikm)
            mac.init(SecretKeySpec(prk, "HmacSHA1"))
            val info = "ss-subkey".toByteArray(Charsets.US_ASCII)
            val out = ArrayList<Byte>(len + 20)
            var t = ByteArray(0)
            var i = 1
            while (out.size < len) {
                mac.reset()
                mac.update(t)
                mac.update(info)
                mac.update(i.toByte())
                t = mac.doFinal()
                out.addAll(t.toList())
                i++
            }
            return out.take(len).toByteArray()
        }
    }
}

/** 支持的 ss 加密方式 → key 长度（salt 长度同 key） */
object SsMethods {
    val SUPPORTED = listOf("aes-256-gcm", "aes-128-gcm", "chacha20-ietf-poly1305")

    fun keyLen(method: String): Int? = when (method) {
        "aes-128-gcm" -> 16
        "aes-256-gcm" -> 32
        "chacha20-ietf-poly1305" -> 32
        else -> null
    }
}
