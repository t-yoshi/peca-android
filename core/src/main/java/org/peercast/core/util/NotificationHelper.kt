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
import android.os.SystemClock
import androidx.annotation.MainThread
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

        startForegroundForStandby(30_000L)
    }

    @MainThread
    fun updateChannel(chId: String, chInfo: ChannelInfo) {
        if (chId !in activeChannelInfo)
            return
        startChannel(chId, chInfo)
    }

    @MainThread
    fun startChannel(chId: String, chInfo: ChannelInfo) {
        activeChannelInfo[chId] = chInfo

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

        startForeground(nb.build())
        jFinishStandby?.cancel()
    }

    private fun startForegroundForStandby(duration: Long = DEFAULT_FOREGROUND_DURATION) {
        require(duration > 0)

        if (activeChannelInfo.isNotEmpty())
            return

        val title = service.getString(R.string.peercast_has_started, service.nativeGetPort())

        val nb = NotificationCompat.Builder(service, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notify_icon)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentTitle(title)
            .setContentIntent(getMainActivityPendingIntent())
        // 通知バー [キャッシュのクリア] ボタン
//            .addAction(
//                R.drawable.ic_notification_clear_cache,
//                service.getText(R.string.clear_cache),
//                getClearCachePendingIntent()
//            )
        startForeground(nb.build())

        jFinishStandby?.cancel()
        jFinishStandby = service.lifecycleScope.launch {
            delayRealTime(duration)
            stopForeground()
            //キャッシュが残っていると接続に時間が掛かることがある
            launch {
                delayRealTime(KEEP_CACHE_DURATION_ON_IDLE)
                service.nativeClearCache()
            }
        }
    }

    @MainThread
    fun removeChannel(chId: String) {
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
        return LibPeerCast.createStreamIntent(chId, service.nativeGetPort(), chInfo)
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

    private fun getClearCachePendingIntent(): PendingIntent {
        return Intent(PeerCastService.ACTION_CLEAR_CACHE)
            .setPackage(service.packageName)
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

    //Android12でフォアグラウンド実行に制限
    private fun startForeground(n: Notification) {
        Timber.d("startForeground")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            //bindServiceで作成されたサービスを、
            // さらにstartServiceして、
            // (再生中または、FOREGROUND_DURATIONの間は)killされにくいサービスにする。
            ContextCompat.startForegroundService(
                service,
                Intent(service, PeerCastService::class.java)
            )
            service.startForeground(NOTIFY_ID, n)
        }
    }

    private fun stopForeground() {
        Timber.d("stopForeground")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            service.stopForeground(true)
            service.stopSelf()
        }
    }

    companion object {
        //実際の時間、delayする
        private suspend fun delayRealTime(ms: Long) {
            if (ms <= 0)
                return

            val t = SystemClock.elapsedRealtime() + ms
            var r = ms
            while (SystemClock.elapsedRealtime() < t && r > 0) {
                val d = ms / 100 + 100
                delay(d)
                r -= d
            }
        }

        private const val NOTIFICATION_CHANNEL_ID = "peercast_id"

        private const val NOTIFY_ID = 0x7144 // 適当

        private const val DEFAULT_FOREGROUND_DURATION = 3 * 60_000L
        private const val KEEP_CACHE_DURATION_ON_IDLE = 10 * 60_000L
    }
}