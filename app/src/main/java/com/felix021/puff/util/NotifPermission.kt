package com.felix021.puff.util

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.felix021.puff.R

/**
 * 通知权限按需申请：首次触发下载时先弹说明框（为什么要通知权限），
 * 用户点「允许」再走系统权限弹窗；拒绝则本会话内不再打扰，
 * 下载照常进行（只是通知栏没有进度/完成提醒）。
 */
object NotifPermission {
    private const val REQ = 7001
    private var declinedThisSession = false
    private var pending: (() -> Unit)? = null

    fun ensure(activity: Activity?, onReady: () -> Unit) {
        if (Build.VERSION.SDK_INT < 33 || isGranted(activity)) { onReady(); return }
        // 本会话拒绝过：不再弹说明，直接继续（下载仍会执行）
        if (declinedThisSession) { onReady(); return }
        if (activity == null) { onReady(); return }
        pending = onReady
        AlertDialog.Builder(activity)
            .setTitle(R.string.notif_rationale_title)
            .setMessage(R.string.notif_rationale_body)
            .setPositiveButton(R.string.notif_allow) { _, _ ->
                activity.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ)
            }
            .setNegativeButton(R.string.notif_later) { _, _ ->
                declinedThisSession = true
                onReady()
            }
            .setOnCancelListener {
                declinedThisSession = true
                onReady()
            }
            .show()
    }

    /** MainActivity.onRequestPermissionsResult 转发进来 */
    fun handleResult(requestCode: Int, granted: Boolean) {
        if (requestCode != REQ) return
        if (!granted) declinedThisSession = true
        pending?.invoke()
        pending = null
    }

    private fun isGranted(activity: Activity?): Boolean =
        activity != null &&
            ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
}
