package org.peercast.core

/**
 * (c) 2014, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import androidx.annotation.BinderThread
import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.peercast.core.lib.PeerCastController
import org.peercast.core.lib.PeerCastRpcClient
import org.peercast.core.lib.internal.NotificationUtils
import org.peercast.core.lib.internal.ServiceIntents
import org.peercast.core.lib.notify.NotifyChannelType
import org.peercast.core.lib.rpc.io.JsonRpcConnection
import org.peercast.core.util.NotificationHelper
import org.peercast.core.util.unzipFile
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

        unzipHtmlDir()
        installDefaultIniFile()

        nativeStart(filesDir.absolutePath)
        notificationHelper = NotificationHelper(this)

        registerReceiver(commandReceiver, IntentFilter().also {
            it.addAction(ACTION_BUMP_CHANNEL)
            it.addAction(ACTION_STOP_CHANNEL)
            it.addAction(ACTION_CLEAR_CACHE)
        })
    }

    private fun unzipHtmlDir() {
        //解凍済みを示す空フォルダ ex: 3.0.0-YT28
        val d = File(filesDir, "${BuildConfig.VERSION_NAME}-${BuildConfig.YT_VERSION}")
        if (d.exists())
            return
        try {
            assets.unzipFile("peca-yt.zip", filesDir)
            d.mkdir()
        } catch (e: IOException) {
            Timber.e(e, "html-dir install failed.")
        }
    }

    private fun installDefaultIniFile() {
        //YPを含むpeercast.iniを用意する
        val f = File(filesDir, "peercast.ini")
        if (f.exists())
            return
        try {
            Timber.i("install default peercast.ini")
            resources.openRawResource(R.raw.default_peercast_ini).use {
                it.copyTo(f.outputStream())
            }
        } catch (e: IOException) {
            Timber.e(e)
        }
    }

    @Deprecated("Obsoleted since v4.0")
    override fun handleMessage(msg: Message): Boolean {
        val reply = serviceHandler.obtainMessage(msg.what)
        when (msg.what) {
            PeerCastController.MSG_GET_APPLICATION_PROPERTIES -> {
                reply.data.putInt("port", nativeGetPort())
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
    private val commandReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            fun runRpc(f: suspend PeerCastRpcClient.(String) -> Unit) = lifecycleScope.launch {
                val conn = JsonRpcConnection(port = nativeGetPort())
                val client = PeerCastRpcClient(conn)
                runCatching {
                    intent.getStringExtra(EX_CHANNEL_ID)?.let {
                        client.f(it)
                    }
                }.onFailure(Timber::e)
            }

            when (intent.action) {
                ACTION_BUMP_CHANNEL -> runRpc { bumpChannel(it) }
                ACTION_STOP_CHANNEL -> runRpc { stopChannel(it) }
                ACTION_CLEAR_CACHE -> nativeClearCache()
                else -> throw IllegalArgumentException("invalid action: $intent")
            }
        }
    }

    private val aidlBinder = object : IPeerCastService.Stub() {
        override fun getVersion() = BuildConfig.VERSION_CODE

        var callbacks = ArrayList<INotificationCallback>()

        @BinderThread
        override fun registerNotificationCallback(callback: INotificationCallback) {
            lifecycleScope.launch {
                callbacks.add(callback)
            }
        }

        @BinderThread
        override fun unregisterNotificationCallback(callback: INotificationCallback) {
            lifecycleScope.launch {
                callbacks.remove(callback)
            }
        }

        @MainThread
        fun fireNotifyChannel(notifyType: Int, chId: String, jsonChannelInfo: String) {
            fireEvent {
                it.onNotifyChannel(notifyType, chId, jsonChannelInfo)
            }
        }

        @MainThread
        fun fireNotifyMessage(notifyType: Int, message: String) {
            fireEvent {
                it.onNotifyMessage(notifyType, message)
            }
        }

        private fun fireEvent(f: (INotificationCallback) -> Unit) {
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

        override fun getPort() = this@PeerCastService.nativeGetPort()

        override fun setPort(port: Int) = this@PeerCastService.nativeSetPort(port)
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

        stopForeground(true)
        nativeQuit()
    }

    /**
     * ネイティブ側から呼ばれる。
     * @see org.peercast.core.lib.notify.NotifyMessageType
     */
    @Suppress("unused")
    private fun notifyMessage(notifyType: Int, message: String) {
        Timber.d("notifyMessage: $notifyType, $message")
        lifecycleScope.launch {
            aidlBinder.fireNotifyMessage(notifyType, message)
        }
    }


    /**
     * ネイティブ側から呼ばれる。
     * @see org.peercast.core.lib.notify.NotifyChannelType
     */
    @Suppress("unused")
    private fun notifyChannel(notifyType: Int, chId: String, jsonChannelInfo: String) {
        Timber.d("notifyChannel: $notifyType $chId $jsonChannelInfo")
        val chInfo = NotificationUtils.jsonToChannelInfo(jsonChannelInfo) ?: return

        // nativeStart時のnotificationHelperが未初期化なときに
        // 到達する場合があるのでimmediateにしない
        lifecycleScope.launch(Dispatchers.Main) {
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
    private external fun nativeSetPort(port: Int)

    external fun nativeGetPort(): Int

    external fun nativeClearCache(cmd: Int = CMD_CLEAR_HOST_CACHE or CMD_CLEAR_HIT_LISTS_CACHE)

    /**
     * PeerCastを終了します。
     */
    private external fun nativeQuit()

    companion object {
        // 通知ボタンAction
        const val ACTION_BUMP_CHANNEL = "org.peercast.core.ACTION.bumpChannel"
        const val ACTION_STOP_CHANNEL = "org.peercast.core.ACTION.stopChannel"
        const val ACTION_CLEAR_CACHE = "org.peercast.core.ACTION.clearCache"

        const val CMD_CLEAR_HOST_CACHE = 1
        const val CMD_CLEAR_HIT_LISTS_CACHE = 2
        const val CMD_CLEAR_CHANNELS_CACHE = 4

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


