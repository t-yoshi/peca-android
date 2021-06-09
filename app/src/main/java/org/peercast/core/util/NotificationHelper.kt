package org.peercast.core.util

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import org.peercast.core.PeerCastService
import org.peercast.core.R
import org.peercast.core.lib.LibPeerCast
import org.peercast.core.lib.rpc.ChannelInfo
import org.peercast.core.preferences.AppPreferences

/**
 * 通知バーのボタン処理用
 *  [コンタクト、再接続、切断]ボタンを表示する。
 * @see PeerCastService.onStartCommand
 * @author (c) 2014-2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
class NotificationHelper(
    private val service: Service,
    private val appPrefs: AppPreferences,
) {
    private val manager = service.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager

    private val activeChannelInfo = LinkedHashMap<String, ChannelInfo>() //key=chanId

    fun update(chId: String, chInfo: ChannelInfo) {
        synchronized(activeChannelInfo) {
            if (chId !in activeChannelInfo)
                return
        }
        start(chId, chInfo)
    }

    fun start(chId: String, chInfo: ChannelInfo) {
        synchronized(activeChannelInfo) {
            activeChannelInfo[chId] = chInfo
        }

        val nb = NotificationCompat.Builder(service, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notify_icon)
            //.setLargeIcon(icon)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)

            // 通知バー タイトル部分
            .setContentTitle("Playing: ${chInfo.name}")
            .setContentText("${chInfo.desc} ${chInfo.comment}")
            .setContentIntent(piPlay(chId, chInfo))

            // 通知バーに [コンタクト、再接続、切断]ボタンを表示する。

            // 通知バー [コンタクト] ボタン
            .addAction(R.drawable.ic_notification_contact_url,
                service.getText(R.string.t_contact),
                piContact(chInfo))

            // 通知バー [再接続] ボタン
            .addAction(R.drawable.ic_notification_bump,
                service.getText(R.string.t_bump), piBump(chId))

            // 通知バー [切断] ボタン
            .addAction(R.drawable.ic_notification_disconnect,
                service.getText(R.string.t_disconnect), piDisconnect(chId))

        service.startForeground(NOTIFY_ID, nb.build())
    }

    fun remove(chId: String) = synchronized(activeChannelInfo) {
        activeChannelInfo.remove(chId)
        if (activeChannelInfo.isEmpty()) {
            service.stopForeground(true)
        } else {
            activeChannelInfo.entries.last().let {
                update(it.key, it.value)
            }
        }
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createNotificationChannel()
    }

    //通知バーのボタンを押すと再生
    private fun piPlay(chId: String, chInfo: ChannelInfo) = PendingIntent.getActivity(service, 0,
        LibPeerCast.createStreamIntent(chId, appPrefs.port, chInfo).also {
            //it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            //it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }, 0)


    // コンタクトURLを開く
    private fun piContact(chInfo: ChannelInfo) = PendingIntent.getActivity(service, 0,
        Intent(Intent.ACTION_VIEW, Uri.parse(chInfo.url)).also {
            //it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
        PendingIntent.FLAG_UPDATE_CURRENT)

    private fun piBump(channelId: String) = PendingIntent.getService(service, 0,
        Intent(PeerCastService.ACTION_BUMP_CHANNEL,
            null,
            service,
            PeerCastService::class.java).also {
            it.putExtra(PeerCastService.EX_CHANNEL_ID, channelId)
        }, PendingIntent.FLAG_UPDATE_CURRENT)

    private fun piDisconnect(channelId: String) = PendingIntent.getService(service, 0,
        Intent(PeerCastService.ACTION_STOP_CHANNEL,
            null,
            service,
            PeerCastService::class.java).also {
            it.putExtra(PeerCastService.EX_CHANNEL_ID, channelId)
        }, PendingIntent.FLAG_UPDATE_CURRENT)


    @TargetApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID, "PeerCast",
            NotificationManager.IMPORTANCE_LOW)
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "peercast_id"

        private const val NOTIFY_ID = 0x7144 // 適当
    }
}
