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
import org.peercast.core.lib.internal.NotificationUtils
import org.peercast.core.lib.internal.ServiceIntents
import org.peercast.core.lib.notify.NotifyChannelType
import org.peercast.core.util.AssetUnzip
import org.peercast.core.util.NotificationHelper
import timber.log.Timber
import java.io.File
import java.io.IOException

class PeerCastService : LifecycleService(), Handler.Callback {

    @Deprecated("Obsoleted since v4.0")
    private val serviceHandler = Handler(Looper.getMainLooper(), this)

    @Deprecated("Obsoleted since v4.0")
    private lateinit var serviceMessenger: Messenger

    private lateinit var notificationHelper: NotificationHelper

    override fun onCreate() {
        super.onCreate()

        serviceMessenger = Messenger(serviceHandler)
        notificationHelper = NotificationHelper(this)

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

        nativeStart(filesDir.absolutePath)
    }

    @Deprecated("Obsoleted since v4.0")
    override fun handleMessage(msg: Message): Boolean {
        val reply = serviceHandler.obtainMessage(msg.what)
        when (msg.what) {
            PeerCastController.MSG_GET_APPLICATION_PROPERTIES -> {
                reply.data.putInt("port", getPort())
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
        val r = super.onStartCommand(intent, flags, startId)
        Timber.d("onStartCommand(intent=$intent, flags=$flags, startId=$startId)")

        val channelId = intent?.getStringExtra(EX_CHANNEL_ID) ?: return r
        val conn = JsonRpcConnection(port = getPort())
        val client = PeerCastRpcClient(conn)
        val f = when (intent.action) {
            ACTION_BUMP_CHANNEL -> client::bumpChannel
            ACTION_STOP_CHANNEL -> client::stopChannel
            else -> {
                { throw IllegalArgumentException("invalid action: $intent") }
            }
        }
        lifecycleScope.launch {
            runCatching {
                f(channelId)
            }.onFailure(Timber::e)
        }
        return START_NOT_STICKY
    }

    private val aidlBinder = object : IPeerCastService.Stub() {
        var callbacks = ArrayList<INotificationCallback>()

        override fun registerNotificationCallback(callback: INotificationCallback) {
            synchronized(callbacks) {
                callbacks.add(callback)
            }
        }

        override fun unregisterNotificationCallback(callback: INotificationCallback) {
            synchronized(callbacks) {
                callbacks.remove(callback)
            }
        }

        fun fireNotifyChannel(notifyType: Int, chId: String, jsonChannelInfo: String) {
            fireEvent {
                it.onNotifyChannel(notifyType, chId, jsonChannelInfo)
            }
        }

        fun fireNotifyMessage(notifyType: Int, message: String) {
            fireEvent {
                it.onNotifyMessage(notifyType, message)
            }
        }

        private fun fireEvent(f: (INotificationCallback) -> Unit) {
            synchronized(callbacks) {
                val it = callbacks.listIterator()
                while (it.hasNext()) {
                    kotlin.runCatching {
                        f(it.next())
                    }.onFailure { e ->
                        when (e) {
                            is RemoteException,
                            is SecurityException,
                            -> {
                                it.remove()
                                Timber.w(e, "remove callback")
                            }
                            else -> throw e
                        }
                    }
                }
            }
        }

        override fun getPort() = this@PeerCastService.getPort()

        override fun setPort(port: Int) = this@PeerCastService.setPort(port)
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        Timber.d("$intent")

        //NOTE: 同じインテントだとBinderはキャッシュされる
        //https://commonsware.com/blog/2011/07/02/about-binder-caching.html
        return when (intent.action) {
            ServiceIntents.ACT_PEERCAST_SERVICE -> serviceMessenger.binder
            ServiceIntents.ACT_PEERCAST_SERVICE4 -> aidlBinder
            else -> null
        }
    }

    override fun onUnbind(intent: Intent): Boolean {
        return false
    }

    override fun onDestroy() {
        super.onDestroy()

        notificationHelper.stopForeground()
        nativeQuit()
    }

    /**
     * ネイティブ側から呼ばれる。
     * @see org.peercast.core.lib.notify.NotifyMessageType
     */
    @Suppress("unused")
    private fun notifyMessage(notifyType: Int, message: String) {
        Timber.d("notifyMessage: $notifyType, $message")
        aidlBinder.fireNotifyMessage(notifyType, message)
    }


    /**
     * ネイティブ側から呼ばれる。
     * @see org.peercast.core.lib.notify.NotifyChannelType
     */
    @Suppress("unused")
    private fun notifyChannel(notifyType: Int, chId: String, jsonChannelInfo: String) {
        Timber.d("notifyChannel: $notifyType $chId $jsonChannelInfo")
        val chInfo = NotificationUtils.jsonToChannelInfo(jsonChannelInfo) ?: return
        when (notifyType) {
            NotifyChannelType.Start.nativeValue ->
                notificationHelper.startChannel(chId, chInfo)
            NotifyChannelType.Update.nativeValue ->
                notificationHelper.updateChannel(chId, chInfo)
            NotifyChannelType.Stop.nativeValue ->
                notificationHelper.removeChannel(chId)
            else -> throw IllegalArgumentException()
        }
        aidlBinder.fireNotifyChannel(notifyType, chId, jsonChannelInfo)
    }

    /**
     * PeerCastを開始します。
     *
     * @param filesDirPath     Context.getFilesDir()
     */
    private external fun nativeStart(filesDirPath: String)

    /**
     * @param port 動作ポート (1025..65532)
     */
    private external fun setPort(port: Int)

    external fun getPort(): Int

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


