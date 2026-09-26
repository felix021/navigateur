package com.felix021.navigateur.data

import android.util.Log
import java.io.DataInputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 本地 Shadowsocks 客户端（ss-local 等价物）：
 * 监听 127.0.0.1 的 SOCKS5 入口，把 CONNECT 的流量按 ss AEAD 协议转给远程服务器。
 * WebView 的 ProxyController 只认 HTTP/SOCKS5，SS 出口通过
 * `socks5://127.0.0.1:<localPort>` 接进来（见 ProxyRepository）。
 *
 * 线程模型：accept 循环 1 条；每条连接 2 个 pump（上行/下行）+ 1 个握手线程，
 * 连接结束自行退出；stop() 关闭 ServerSocket 使 accept 返回。
 */
class SsTunnel {

    @Volatile private var server: ServerSocket? = null
    @Volatile private var running = false
    private val generation = AtomicBoolean(false)

    /** 当前配置指纹（host:port:method:password），避免同配置反复重启 */
    @Volatile private var fingerprint = ""

    val localPort: Int get() = server?.localPort ?: 0
    val isRunning: Boolean get() = running

    /** 配置没变则复用；变了则重启。返回本地端口（失败 0） */
    @Synchronized
    fun ensure(profile: ProxyProfile): Int {
        val key = "${profile.host}:${profile.port}:${profile.method}:${profile.password}"
        if (running && key == fingerprint) return localPort
        stopLocked()
        val masterKey = SsCipher.masterKeyOf(profile.method, profile.password.toByteArray(Charsets.UTF_8))
        if (masterKey == null) {
            Log.w("NavigateurSs", "unsupported method ${profile.method}")
            return 0
        }
        return try {
            val ss = ServerSocket()
            ss.reuseAddress = true
            ss.bind(InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 64)
            server = ss
            fingerprint = key
            running = true
            generation.set(true)
            val gen = generation.get()
            Thread({
                acceptLoop(ss, profile, masterKey, gen)
            }, "ss-accept").apply { isDaemon = true; start() }
            ss.localPort
        } catch (e: Exception) {
            Log.w("NavigateurSs", "start failed", e)
            running = false
            0
        }
    }

    @Synchronized
    fun stop() = stopLocked()

    private fun stopLocked() {
        running = false
        generation.set(false)
        fingerprint = ""
        runCatching { server?.close() }
        server = null
    }

    private fun acceptLoop(ss: ServerSocket, profile: ProxyProfile, masterKey: ByteArray, gen: Boolean) {
        while (running && gen == generation.get()) {
            val sock = try {
                ss.accept()
            } catch (_: Exception) {
                break
            }
            Thread({
                try {
                    handle(sock, profile, masterKey)
                } catch (e: Exception) {
                    Log.d("NavigateurSs", "conn end: $e")
                } finally {
                    runCatching { sock.close() }
                }
            }, "ss-conn").apply { isDaemon = true; start() }
        }
    }

    private fun handle(local: Socket, profile: ProxyProfile, masterKey: ByteArray) {
        local.tcpNoDelay = true
        val input = DataInputStream(local.getInputStream())
        val output = local.getOutputStream()

        // ---- SOCKS5 握手：[VER NMETHODS METHODS] → [VER METHOD] ----
        val ver = input.read()
        if (ver != 5) return
        val n = input.read()
        repeat(n) { input.read() }
        output.write(byteArrayOf(5, 0))
        output.flush()

        // ---- 请求：[VER CMD RSV ATYP ADDR PORT] ----
        input.read() // VER(5)
        val cmd = input.read()
        input.read() // RSV
        val atyp = input.read()
        // ss header 的目标地址格式 = [ATYP][ADDR][PORT]，ATYP 必须随地址写入
        val target: ByteArray = when (atyp) {
            1 -> {
                val b = ByteArray(4); input.readFully(b)
                byteArrayOf(1) + b
            }
            4 -> {
                val b = ByteArray(16); input.readFully(b)
                byteArrayOf(4) + b
            }
            else -> { // 3 = domain
                val len = input.read()
                val b = ByteArray(len); input.readFully(b)
                byteArrayOf(3, len.toByte()) + b
            }
        }
        val portBytes = ByteArray(2); input.readFully(portBytes)
        if (cmd != 1) { // 仅支持 CONNECT
            output.write(byteArrayOf(5, 7, 0, 1, 0, 0, 0, 0, 0, 0)); output.flush()
            return
        }

        // ---- 连远程 ss 服务器 ----
        val remote = Socket()
        try {
            remote.tcpNoDelay = true
            remote.connect(InetSocketAddress(profile.host, profile.port), 10_000)
        } catch (e: Exception) {
            output.write(byteArrayOf(5, 5, 0, 1, 0, 0, 0, 0, 0, 0)); output.flush()
            throw e
        }

        // ---- ss 握手：方向1（上行）salt + 加密的目标地址 ----
        val up = SsCipher(profile.method, masterKey)
        val remoteOut = remote.getOutputStream()
        remoteOut.write(up.newSalt())
        remoteOut.write(up.seal(target + portBytes))
        remoteOut.flush()

        // SOCKS5 连接成功回复
        output.write(byteArrayOf(5, 0, 0, 1, 0, 0, 0, 0, 0, 0))
        output.flush()

        // ---- 中继：上行（本地→远程）分块加密 ----
        val down = SsCipher(profile.method, masterKey)
        val remoteIn = DataInputStream(remote.getInputStream())
        val upThread = Thread({
            try {
                val buf = ByteArray(16 * 1024)
                while (true) {
                    val r = input.read(buf)
                    if (r < 0) break
                    var off = 0
                    while (off < r) {
                        val n = minOf(0x3FFF, r - off)
                        remoteOut.write(up.seal(buf.copyOfRange(off, off + n)))
                        off += n
                    }
                    remoteOut.flush()
                }
            } catch (_: Exception) {
            } finally {
                runCatching { remote.shutdownOutput() }
            }
        }, "ss-up").apply { isDaemon = true }

        // ---- 中继：下行（远程→本地）解密后写回 ----
        val downThread = Thread({
            try {
                val salt = ByteArray(SsMethods.keyLen(profile.method) ?: 32)
                remoteIn.readFully(salt)
                down.setSalt(salt)
                while (true) {
                    val len = remoteIn.readUnsignedShort()
                    val block = ByteArray(len)
                    remoteIn.readFully(block)
                    val plain = down.open(block)
                    output.write(plain)
                    output.flush()
                }
            } catch (_: EOFException) {
            } catch (_: Exception) {
            } finally {
                runCatching { local.shutdownOutput() }
            }
        }, "ss-down").apply { isDaemon = true }

        upThread.start()
        downThread.start()
        upThread.join()
        downThread.join()
        runCatching { remote.close() }
    }
}
