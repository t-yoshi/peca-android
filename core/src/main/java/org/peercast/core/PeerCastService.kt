package org.peercast.core

/**
 * (c) 2014, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */

import android.content.Intent
import android.os.*
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.peercast.core.common.AppPreferences
import org.peercast.core.lib.JsonRpcConnection
import org.peercast.core.lib.PeerCastController
import org.peercast.core.lib.PeerCastRpcClient
import org.peercast.core.lib.internal.PeerCastNotification
import org.peercast.core.lib.notify.NotifyChannelType
import org.peercast.core.util.AssetUnzip
import org.peercast.core.util.NotificationHelper
import org.peercast.pecaport.PecaPort
import timber.log.Timber
import java.io.File
import java.io.IOException

class PeerCastService : LifecycleService(), Handler.Callback {

    private val serviceHandler = Handler(Looper.getMainLooper(), this)
    private lateinit var serviceMessenger: Messenger

    private val appPrefs by inject<AppPreferences>()
    private lateinit var notificationHelper: NotificationHelper

    override fun onCreate() {
        super.onCreate()

        serviceMessenger = Messenger(serviceHandler)
        notificationHelper = NotificationHelper(this, appPrefs)

        //解凍済みを示す空フォルダ ex: 3.0.0-YT28
        val extracted = File(filesDir, "${BuildConfig.VERSION_NAME}-${BuildConfig.YT_VERSION}")
        if (!extracted.exists()) {
            try {
                AssetUnzip.doExtract(this@PeerCastService,
                    "peca-yt.zip",
                    filesDir)
                extracted.mkdir()
            } catch (e: IOException) {
                Timber.e(e, "html-dir install failed.")
            }
        }

        //YPを含むpeercast.iniを用意する
        val iniFile = File(filesDir, "peercast.ini")
        if (!iniFile.exists()) {
            try {
                Timber.i("install default peercast.ini")
                resources.openRawResource(R.raw.default_peercast_ini)
                    .copyTo(iniFile.outputStream())
            } catch (e: IOException) {
                Timber.e(e)
            }
        }

        nativeStart(filesDir.absolutePath, appPrefs.port)

        if (appPrefs.isUPnPEnabled) {
            PecaPort.openPort(this, appPrefs.port)
        }
    }

    override fun handleMessage(msg: Message): Boolean {
        val reply = serviceHandler.obtainMessage(msg.what)
        when (msg.what) {
            PeerCastController.MSG_GET_APPLICATION_PROPERTIES -> {
                reply.data.putInt("port", appPrefs.port)
            }
            else -> {
                Timber.e("Illegal value: msg.what=${msg.what}")
                return false
            }
        }
        try {
            msg.replyTo?.send(reply)
        } catch (e: RemoteException) {
            Timber.e(e, "msg.replyTo.send(reply)")
        }
        return true
    }

    /**
     * 通知バーのボタンのイベントを処理する。
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("onStartCommand(intent=$intent, flags=$flags, startId=$startId)")

        val channelId = intent?.getStringExtra(EX_CHANNEL_ID)
            ?: return super.onStartCommand(intent, flags, startId)
        val conn = JsonRpcConnection(port = appPrefs.port)
        val client = PeerCastRpcClient(conn)
        val f = when (intent.action) {
            ACTION_BUMP_CHANNEL -> client::bumpChannel
            ACTION_STOP_CHANNEL -> client::stopChannel
            else -> return super.onStartCommand(intent, flags, startId)
        }
        lifecycleScope.launch {
            runCatching {
                f(channelId)
            }.onFailure(Timber::e)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return serviceMessenger.binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        return false
    }

    override fun onDestroy() {
        super.onDestroy()

        if (appPrefs.isUPnPEnabled &&
            appPrefs.isUPnPCloseOnExit
        ) {
            PecaPort.closePort(this, appPrefs.port)
        }

        stopForeground(true)
        nativeQuit()
    }

    /**
     * ネイティブ側から呼ばれる。
     * @see org.peercast.core.lib.notify.NotifyMessageType
     */
    @Suppress("unused")
    private fun notifyMessage(notifyType: Int, message: String) {
//        if (BuildConfig.DEBUG) {
//            Timber.i("notifyMessage: $notifyType, $message")
//        }
        PeerCastNotification.sendBroadCastNotifyMessage(this, notifyType, message)
    }


    /**
     * ネイティブ側から呼ばれる。
     * @see org.peercast.core.lib.notify.NotifyChannelType
     */
    @Suppress("unused")
    private fun notifyChannel(notifyType: Int, chId: String, jsonChannelInfo: String) {
        val chInfo = PeerCastNotification.jsonToChannelInfo(jsonChannelInfo) ?: return
        when (notifyType) {
            NotifyChannelType.Start.nativeValue ->
                notificationHelper.start(chId, chInfo)
            NotifyChannelType.Update.nativeValue ->
                notificationHelper.update(chId, chInfo)
            NotifyChannelType.Stop.nativeValue ->
                notificationHelper.remove(chId)
            else -> throw IllegalArgumentException()
        }
        PeerCastNotification.sendBroadCastNotifyChannel(this, notifyType, chId, jsonChannelInfo)
        //Timber.d("$notifyType $chId $chInfo ${Thread.currentThread()}")
    }

    /**
     * PeerCastを開始します。
     *
     * @param filesDirPath     Context.getFilesDir()
     * @param port 動作ポート
     */
    private external fun nativeStart(filesDirPath: String, port: Int)

    /**
     * PeerCastを終了します。
     */
    private external fun nativeQuit()

    companion object {
        // 通知ボタンAction
        const val ACTION_BUMP_CHANNEL = "org.peercast.core.ACTION.bumpChannel"
        const val ACTION_STOP_CHANNEL = "org.peercast.core.ACTION.stopChannel"

        /**(String)*/
        const val EX_CHANNEL_ID = "channelId"

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

}


