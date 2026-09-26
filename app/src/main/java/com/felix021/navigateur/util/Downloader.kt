package com.felix021.navigateur.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.widget.Toast
import com.felix021.navigateur.R

/** 系统下载：走 DownloadManager，通知栏显示进度，默认落到公共下载目录 */
object Downloader {

    fun start(
        context: Context,
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
    ) {
        try {
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
            val request = DownloadManager.Request(Uri.parse(url))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setTitle(fileName)
            // 公共下载目录：DownloadProvider 是特权写方，Q+ 也无需存储权限；
            // 个别 ROM 会拒绝显式路径，回退到系统默认目标
            try {
                request.setDestinationInExternalPublicDir(
                    android.os.Environment.DIRECTORY_DOWNLOADS, fileName,
                )
            } catch (e: Exception) {
                android.util.Log.w("NavigateurDL", "显式下载目录失败，用默认", e)
            }
            userAgent?.takeIf { it.isNotBlank() }?.let { request.addRequestHeader("User-Agent", it) }
            // 带上当前会话 Cookie，覆盖登录态下载场景
            runCatching {
                CookieManager.getInstance().getCookie(url)?.let { request.addRequestHeader("Cookie", it) }
            }
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(context, context.getString(R.string.download_started), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.util.Log.w("NavigateurDL", "下载失败", e)
            Toast.makeText(context, context.getString(R.string.download_failed, e.message), Toast.LENGTH_SHORT).show()
        }
    }
}
