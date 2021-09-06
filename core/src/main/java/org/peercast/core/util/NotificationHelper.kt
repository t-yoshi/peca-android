package org.peercast.core.util

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.peercast.core.PeerCastService
import org.peercast.core.R
import org.peercast.core.lib.LibPeerCast
import org.peercast.core.lib.rpc.ChannelInfo
import timber.log.Timber

/**
 * 通知バーのボタン処理用
 *  [コンタクト、再接続、切断]ボタンを表示する。
 * @see PeerCastService.onStartCommand
 * @author (c) 2014-2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
internal class NotificationHelper(
    private val service: PeerCastService
) {
    private val manager = service.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager

    private val activeChannelInfo = LinkedHashMap<String, ChannelInfo>() //key=chanId

    //起動直後または視聴終了後に数分間、通知バーに常駐する
    private var jFinishStandby: Job? = null

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createNotificationChannel()

        startForegroundForStandby()
    }

    fun updateChannel(chId: String, chInfo: ChannelInfo) {
        synchronized(activeChannelInfo) {
            if (chId !in activeChannelInfo)
                return
        }
        startChannel(chId, chInfo)
    }

    fun startChannel(chId: String, chInfo: ChannelInfo) {
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
            .setContentIntent(getPlayPendingIntent(chId, chInfo))

            // 通知バーに [コンタクト、再接続、切断]ボタンを表示する。

            // 通知バー [コンタクト] ボタン
            .addAction(
                R.drawable.ic_notification_contact_url,
                service.getText(R.string.contact),
                getContactPendingIntent(chInfo)
            )

            // 通知バー [再接続] ボタン
            .addAction(
                R.drawable.ic_notification_bump,
                service.getText(R.string.bump), getBumpPendingIntent(chId)
            )

            // 通知バー [切断] ボタン
            .addAction(
                R.drawable.ic_notification_disconnect,
                service.getText(R.string.disconnect), getDisconnectPendingIntent(chId)
            )
            .setDeleteIntent(getDisconnectPendingIntent(chId))

        startForeground(nb.build())
        jFinishStandby?.cancel()
    }

    private fun startForegroundForStandby() {
        val title = service.getString(R.string.peercast_has_started, service.getPort())

        val nb = NotificationCompat.Builder(service, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notify_icon)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentTitle(title)
            .setContentIntent(getMainActivityPendingIntent())
        startForeground(nb.build())

        jFinishStandby?.cancel()
        jFinishStandby = service.lifecycleScope.launch {
            delay(FOREGROUND_DURATION)
            stopForeground()
        }
    }

    fun removeChannel(chId: String) = synchronized(activeChannelInfo) {
        activeChannelInfo.remove(chId)
        if (activeChannelInfo.isEmpty()) {
            startForegroundForStandby()
        } else {
            activeChannelInfo.entries.last().let {
                updateChannel(it.key, it.value)
            }
        }
    }

    //ui.PeerCastActivityを起動
    private fun getMainActivityPendingIntent(): PendingIntent {
        return Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setClassName(service.packageName, "org.peercast.core.ui.PeerCastActivity")
            .toPendingIntentForActivity()
    }

    //通知バーのボタンを押すと再生
    private fun getPlayPendingIntent(chId: String, chInfo: ChannelInfo): PendingIntent {
        return LibPeerCast.createStreamIntent(chId, service.getPort(), chInfo)
            .toPendingIntentForActivity()
    }

    // コンタクトURLを開く
    private fun getContactPendingIntent(chInfo: ChannelInfo): PendingIntent {
        return Intent(Intent.ACTION_VIEW, Uri.parse(chInfo.url))
            .toPendingIntentForActivity()
    }

    private fun getBumpPendingIntent(channelId: String): PendingIntent {
        return Intent(PeerCastService.ACTION_BUMP_CHANNEL)
            .setPackage(service.packageName)
            .putExtra(PeerCastService.EX_CHANNEL_ID, channelId)
            .toPendingIntentForBroadcast()
    }

    private fun getDisconnectPendingIntent(channelId: String): PendingIntent {
        return Intent(PeerCastService.ACTION_STOP_CHANNEL)
            .setPackage(service.packageName)
            .putExtra(PeerCastService.EX_CHANNEL_ID, channelId)
            .toPendingIntentForBroadcast()
    }

    private fun Intent.toPendingIntentForActivity(): PendingIntent {
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        @TargetApi(Build.VERSION_CODES.M)
        flags = flags or PendingIntent.FLAG_IMMUTABLE

        return PendingIntent.getActivity(
            service, 0, this, flags
        )
    }

    private fun Intent.toPendingIntentForBroadcast(): PendingIntent {
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        @TargetApi(Build.VERSION_CODES.M)
        flags = flags or PendingIntent.FLAG_IMMUTABLE

        return PendingIntent.getBroadcast(
            service, 0, this, flags
        )
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID, "PeerCast",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    private fun startForeground(n: Notification) {
        Timber.d("startForeground")
        //bindServiceで作成されたサービスを、
        // さらにstartServiceして、
        // (再生中または、FOREGROUND_DURATIONの間は)killされにくいサービスにする。
        ContextCompat.startForegroundService(
            service,
            Intent(service, PeerCastService::class.java)
        )
        service.startForeground(NOTIFY_ID, n)
    }

    fun stopForeground() {
        Timber.d("stopForeground")
        service.stopForeground(true)
        jFinishStandby?.cancel()
        service.stopSelf()
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "peercast_id"

        private const val NOTIFY_ID = 0x7144 // 適当

        private const val FOREGROUND_DURATION = 5 * 60_000L
    }
}