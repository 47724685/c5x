package com.carlauncher.service

import android.annotation.TargetApi
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * 媒体通知监听服务 —— 需要 API 18+
 * API 14/15/16/17 设备此服务不会启动，媒体控制区域会隐藏。
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class MediaNotificationService : NotificationListenerService() {

    companion object {
        const val ACTION_MEDIA_UPDATE = "com.carlauncher.MEDIA_UPDATE"
        const val EXTRA_TITLE        = "title"
        const val EXTRA_ARTIST       = "artist"
        const val EXTRA_IS_PLAYING   = "is_playing"

        // 简易全局媒体控制指令
        const val ACTION_CMD         = "com.carlauncher.MEDIA_CMD"
        const val EXTRA_CMD          = "cmd"   // "play","pause","next","prev"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        broadcastMediaState("", "", false)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        broadcastMediaState("", "", false)
    }

    private fun broadcastMediaState(title: String, artist: String, playing: Boolean) {
        val intent = Intent(ACTION_MEDIA_UPDATE).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_ARTIST, artist)
            putExtra(EXTRA_IS_PLAYING, playing)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }
}
