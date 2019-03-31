package org.peercast.core

/**
 * (c) 2014, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.*
import android.util.SparseArray
import androidx.core.app.NotificationCompat
import androidx.core.util.isEmpty
import org.koin.android.ext.android.inject
import org.peercast.core.PeerCastController.Companion.MSG_CMD_CHANNEL_BUMP
import org.peercast.core.PeerCastController.Companion.MSG_CMD_CHANNEL_DISCONNECT
import org.peercast.core.PeerCastController.Companion.MSG_CMD_CHANNEL_KEEP_NO
import org.peercast.core.PeerCastController.Companion.MSG_CMD_CHANNEL_KEEP_YES
import org.peercast.core.PeerCastController.Companion.MSG_CMD_SERVENT_DISCONNECT
import org.peercast.core.PeerCastController.Companion.MSG_GET_APPLICATION_PROPERTIES
import org.peercast.core.PeerCastController.Companion.MSG_GET_CHANNELS
import org.peercast.core.PeerCastController.Companion.MSG_GET_STATS
import org.peercast.pecaport.PecaPort
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.zip.ZipInputStream

class PeerCastService : Service() {

    private lateinit var serviceMessenger: Messenger
    private lateinit var serviceHandler: ServiceHandler

    private val appPrefs by inject<AppPreferences>()
    private var runningPort = 0

    private lateinit var notificationHelper: NotificationHelper

    /**
     * PeerCastController からの
     * .sendCommand() .sendChannelCommand() を処理するHandler。
     */
    private inner class ServiceHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            val reply = obtainMessage(msg.what)
            var result: Boolean? = null
            var data = Bundle()
            when (msg.what) {
                MSG_GET_APPLICATION_PROPERTIES -> data = nativeGetApplicationProperties()

                MSG_GET_CHANNELS -> data = nativeGetChannels()

                MSG_GET_STATS -> data = nativeGetStats()

                MSG_CMD_CHANNEL_BUMP,
                MSG_CMD_CHANNEL_DISCONNECT,
                MSG_CMD_CHANNEL_KEEP_YES,
                MSG_CMD_CHANNEL_KEEP_NO -> result = nativeChannelCommand(msg.what, msg.arg1)

                MSG_CMD_SERVENT_DISCONNECT -> result = nativeDisconnectServent(msg.arg1)

                else -> {
                    Timber.e("Illegal value: msg.what=${msg.what}")
                    return
                }
            }

            if (msg.obj === MSG_OBJ_CALL_STOP_SELF) {
                stopSelf(msg.arg2)
                return
            }

            if (msg.replyTo == null) {
                return
            }

            if (result != null)
                data.putBoolean("result", result)

            reply.data = data
            try {
                msg.replyTo.send(reply)
            } catch (e: RemoteException) {
                Timber.e(e, "msg.replyTo.send(reply)")
            }

        }
    }

    override fun onCreate() {
        val thread = HandlerThread(TAG)
        thread.start()

        serviceHandler = ServiceHandler(thread.looper)
        serviceMessenger = Messenger(serviceHandler)
        notificationHelper = NotificationHelper(this)

        try {
            val res = HtmlResource(this)
            if (!res.isInstalled)
                res.doExtract()
        } catch (e: IOException) {
            Timber.e(e, "html-dir install failed.")
            return
        }

        runningPort = nativeStart(
                File(filesDir, "peercast.ini").absolutePath,
                filesDir.absolutePath
        )

        if (appPrefs.isUPnPEnabled && runningPort > 0) {
            PecaPort.openPort(runningPort)
        }
    }

    /**
     * 通知バーのボタンのイベントを処理する。
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("onStartCommand(intent=$intent, flags=$flags, startId=$startId)")

        val msg = serviceHandler.obtainMessage(-1)
        msg.what = when (intent?.action) {
            ACTION_CHANNEL_BUMP -> MSG_CMD_CHANNEL_BUMP
            ACTION_CHANNEL_DISCONNECT -> MSG_CMD_CHANNEL_DISCONNECT
            ACTION_FOREGROUND_STARTED -> {
                Timber.i("Started foreground service.")
                return START_NOT_STICKY
            }
            else -> {
                Timber.e("Illegal intent: $intent")
                return START_NOT_STICKY
            }
        }
        msg.arg1 = intent.getIntExtra("channel_id", -1)
        msg.obj = MSG_OBJ_CALL_STOP_SELF
        msg.arg2 = startId
        serviceHandler.sendMessage(msg)
        return START_NOT_STICKY
    }

    override fun onBind(i: Intent): IBinder? {
        return serviceMessenger.binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        return false
    }

    override fun onDestroy() {
        serviceHandler.looper.quit()

        if (runningPort > 0 &&
                appPrefs.isUPnPEnabled &&
                appPrefs.isUPnPCloseOnExit) {
            PecaPort.closePort(runningPort)
        }

        stopForeground(true)
        nativeQuit()
    }

    /**
     * ネイティブ側から呼ばれる。<br></br>
     * AndroidPeercastApp::notifyMessage( ServMgr::NOTIFY_TYPE tNotify, const char *message)
     * @see NOTIFY_CHANNEL_START
     * @see NOTIFY_CHANNEL_UPDATE
     * @see NOTIFY_CHANNEL_STOP
     */
    private fun notifyMessage(notifyType: Int, message: String) {
        if (BuildConfig.DEBUG) {
            Timber.i("notifyMessage: $notifyType, $message")
        }
    }

    /**
     * ネイティブ側から呼ばれる。<br></br>
     *
     * <pre>
     * AndroidPeercastApp::
     * channelStart(ChanInfo *info)
     * channelUpdate(ChanInfo *info)
     * channelStop(ChanInfo *info)
     * </pre>
     *
     * @see NOTIFY_CHANNEL_START
     * @see NOTIFY_CHANNEL_UPDATE
     * @see NOTIFY_CHANNEL_STOP
     * *
     */
    private fun notifyChannel(notifyType: Int, bChInfo: Bundle, channel_id: Int) {
        val chInfo = ChannelInfo(bChInfo)
        when (notifyType) {
            NOTIFY_CHANNEL_START,
            NOTIFY_CHANNEL_UPDATE -> {
                if (chInfo.type != ChannelInfo.T_UNKNOWN)
                    notificationHelper.update(channel_id, chInfo)
            }
            else -> notificationHelper.remove(channel_id)
        }

        //パワーセーブ: 無駄なリレーを行わない
        if (notifyType == NOTIFY_CHANNEL_START && appPrefs.isPowerSaveMode)
            nativeChannelCommand(MSG_CMD_CHANNEL_KEEP_NO, channel_id)

        if (BuildConfig.DEBUG)
            Timber.i("$chInfo ${Thread.currentThread()}")
    }

    /**
     * PeerCastを開始します。
     *
     * @param iniPath     peercast.iniのパス(要・書き込み可能)
     * @param resourceDir htmlディレクトリのある場所
     * @return 動作ポート
     */
    private external fun nativeStart(iniPath: String, resourceDir: String): Int

    /**
     * PeerCastを終了します。
     */
    private external fun nativeQuit()

    /**
     * 現在アクティブなChannel情報を取得します。
     *
     * @return Bundle。1つも無い場合はnull。2つ目以降はnextキーにリンクリスト形式で収納。
     */
    private external fun nativeGetChannels(): Bundle

    /**
     * 通信量の状態を取得します。
     *
     * @return Bundle
     * @see Stats
     */
    private external fun nativeGetStats(): Bundle

    /**
     * 動作中のポート番号などを取得します。
     *
     * @return Bundle
     */
    private external fun nativeGetApplicationProperties(): Bundle

    /**
     * チャンネルの再接続、キープの有無を操作する。
     *
     * @return 成功した場合 true
     */
    private external fun nativeChannelCommand(cmdType: Int, channel_id: Int): Boolean

    /**
     * 指定したサーヴァントを切断する。
     *
     * @return 成功した場合 true
     */
    private external fun nativeDisconnectServent(servent_id: Int): Boolean


    companion object {
        private const val TAG = "PeerCastService"

        /**
         * arg2=startId
         */
        private val MSG_OBJ_CALL_STOP_SELF = Any()

        private const val NOTIFY_CHANNEL_START = 0
        private const val NOTIFY_CHANNEL_UPDATE = 1
        private const val NOTIFY_CHANNEL_STOP = 2

        // 通知ボタンAction
        private const val ACTION_CHANNEL_BUMP = "org.peercast.core.ACTION_CHANNEL_BUMP"
        private const val ACTION_CHANNEL_DISCONNECT = "org.peercast.core.ACTION_CHANNEL_DISCONNECT"

        private const val ACTION_FOREGROUND_STARTED = "org.peercast.core.ACTION_FOREGROUND_STARTED"
        /**
         * クラス初期化に呼ぶ。
         */
        @JvmStatic
        private external fun nativeClassInit()

        init {
            System.loadLibrary("peercast")
            nativeClassInit()
        }
    }

    private class HtmlResource(c: Context) {
        private val destDir = c.filesDir
        //解凍済みを示す.IM45フォルダ
        private val installedMarkDir = File(destDir, ".IM45")
        private val assetManager = c.assets

        val isInstalled: Boolean
            get() = installedMarkDir.exists()

        /**
         * peca.zipからhtmlフォルダを解凍して /data/data/org.peercast.core/ にインストール。<br></br>
         * 成功した場合は解凍済みを示す .IM45フォルダを作成する。
         */
        fun doExtract() {
            ZipInputStream(assetManager.open("peca.zip")).use { zis ->
                generateSequence { zis.nextEntry }.forEach { ze ->
                    val out = File(destDir, ze.name)
                    Timber.i("Unzipped: $out")
                    if (ze.isDirectory) {
                        if (!out.exists())
                            out.mkdirs()
                    } else {
                        out.outputStream().use { os ->
                            zis.copyTo(os)
                        }
                    }
                    zis.closeEntry()
                }
            }

            installedMarkDir.mkdir()
        }
    }


    /**
     * 通知バーのボタン処理用
     *  [コンタクト、再接続、切断]ボタンを表示する。
     * @see PeerCastService.onStartCommand
     */
    private class NotificationHelper(private val service: PeerCastService) {
        private val manager = service.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        private val activeChannelInfo = SparseArray<ChannelInfo>() //key=id

        fun update(channel_id: Int, chInfo: ChannelInfo) {
            synchronized(activeChannelInfo) {
                activeChannelInfo.put(channel_id, chInfo)
            }
            val nb = NotificationCompat.Builder(service, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notify_icon)
                    //.setLargeIcon(icon)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setPriority(NotificationCompat.PRIORITY_LOW)

                    // 通知バー タイトル部分
                    .setContentTitle("Playing: ${chInfo.name}")
                    .setContentText("${chInfo.desc} ${chInfo.comment}")
                    .setContentIntent(piPlay(chInfo))

                    // 通知バーに [コンタクト、再接続、切断]ボタンを表示する。

                    // 通知バー [コンタクト] ボタン
                    .addAction(R.drawable.ic_notification_contact_url,
                            service.getText(R.string.t_contact),
                            piContact(chInfo))

                    // 通知バー [再接続] ボタン
                    .addAction(R.drawable.ic_notification_bump,
                            service.getText(R.string.t_bump), piBump(channel_id))

                    // 通知バー [切断] ボタン
                    .addAction(R.drawable.ic_notification_disconnect,
                            service.getText(R.string.t_disconnect), piDisconnect(channel_id))

            service.startForeground(NOTIFY_ID, nb.build())
        }

        fun remove(channel_id: Int) = synchronized(activeChannelInfo) {
            activeChannelInfo.remove(channel_id)
            if (activeChannelInfo.isEmpty()) {
                service.stopForeground(true)
            } else {
                activeChannelInfo.let {
                    val i = it.size() - 1
                    update(it.keyAt(i), it.valueAt(i))
                }
            }
        }

        init {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                createNotificationChannel()
        }

        //通知バーのボタンを押すと再生
        private fun piPlay(chInfo: ChannelInfo) = PendingIntent.getActivity(service, 0,
                Intent(Intent.ACTION_VIEW, chInfo.toStreamUrl(service.runningPort)).also {
                    it.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
                }, 0)


        // コンタクトURLを開く
        private fun piContact(chInfo: ChannelInfo) = PendingIntent.getActivity(service, 0,
                Intent(Intent.ACTION_VIEW, Uri.parse(chInfo.url)).also {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
                PendingIntent.FLAG_UPDATE_CURRENT)

        private fun piBump(channel_id: Int) = PendingIntent.getService(service, 0,
                Intent(ACTION_CHANNEL_BUMP, null, service, PeerCastService::class.java).also {
                    it.putExtra("channel_id", channel_id)
                }, PendingIntent.FLAG_UPDATE_CURRENT)

        private fun piDisconnect(channel_id: Int) = PendingIntent.getService(service, 0,
                Intent(ACTION_CHANNEL_DISCONNECT, null, service, PeerCastService::class.java).also {
                    it.putExtra("channel_id", channel_id)
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

}


