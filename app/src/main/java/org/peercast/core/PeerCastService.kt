package org.peercast.core

/**
 * (c) 2014, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */

import android.app.Service
import android.content.Intent
import android.os.*
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import org.peercast.core.lib.LibPeerCast
import org.peercast.core.lib.PeerCastController
import org.peercast.core.lib.PeerCastController.Companion.EX_REQUEST
import org.peercast.core.lib.PeerCastController.Companion.EX_RESPONSE
import org.peercast.core.lib.RpcHttpHostConnection
import org.peercast.core.util.AssetUnzip
import org.peercast.core.util.JsonRpcUtil
import org.peercast.core.util.NotificationHelper
import org.peercast.pecaport.PecaPort
import timber.log.Timber
import java.io.File
import java.io.IOException

class PeerCastService : Service() {

    private lateinit var serviceMessenger: Messenger
    private lateinit var serviceHandler: ServiceHandler

    val appPrefs by inject<AppPreferences>()
    private lateinit var notificationHelper: NotificationHelper

    /**
     * PeerCastControllerからのRPCリクエストを処理するHandler。
     */
    private inner class ServiceHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            val reply = obtainMessage(msg.what)
            when (msg.what) {
                PeerCastController.MSG_PEERCAST_STATION_RPC -> {
                    val jsRequest = msg.data.getString(EX_REQUEST, "")
                    Timber.d("$EX_REQUEST: $jsRequest")
                    val jsResponse = runBlocking {
                        RpcHttpHostConnection("localhost", appPrefs.port).executeRpc(jsRequest)
                    }
                    reply.data.putString(EX_RESPONSE, jsResponse)
                    Timber.d("$EX_RESPONSE: $jsResponse")
                }

                PeerCastController.MSG_GET_APPLICATION_PROPERTIES -> {
                    reply.data.putInt("port", appPrefs.port)
                }

                //v3.0で廃止
                PeerCastController.MSG_GET_CHANNELS,
                PeerCastController.MSG_GET_STATS,
                PeerCastController.MSG_CMD_CHANNEL_BUMP,
                PeerCastController.MSG_CMD_CHANNEL_DISCONNECT,
                PeerCastController.MSG_CMD_CHANNEL_KEEP_YES,
                PeerCastController.MSG_CMD_CHANNEL_KEEP_NO,
                PeerCastController.MSG_CMD_SERVENT_DISCONNECT -> {
                    Timber.e("Obsoleted API: msg.what=${msg.what}")
                    return
                }
                else -> {
                    Timber.e("Illegal value: msg.what=${msg.what}")
                    return
                }
            }

            try {
                msg.replyTo?.send(reply)
            } catch (e: RemoteException) {
                Timber.e(e, "msg.replyTo.send(reply)")
            }

            if (msg.obj === MSG_OBJ_CALL_STOP_SELF)
                stopSelf(msg.arg2)
        }
    }

    override fun onCreate() {
        val thread = HandlerThread(TAG)
        thread.start()

        serviceHandler = ServiceHandler(thread.looper)
        serviceMessenger = Messenger(serviceHandler)
        notificationHelper = NotificationHelper(this)

        try {
            //解凍済みを示すXXX-YT28フォルダ
            val extracted = File(filesDir, "${BuildConfig.VERSION_NAME}-${BuildConfig.YT_VERSION}")
            val unzip = AssetUnzip(assets)
            if (!extracted.exists()) {
                unzip.doExtract("peca-yt.zip", filesDir)
                extracted.mkdir()
            }
        } catch (e: IOException) {
            Timber.e(e, "html-dir install failed.")
            return
        }

        nativeStart(filesDir.absolutePath, appPrefs.port)

        if (appPrefs.isUPnPEnabled) {
            PecaPort.openPort(appPrefs.port)
        }
    }

    /**
     * 通知バーのボタンのイベントを処理する。
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("onStartCommand(intent=$intent, flags=$flags, startId=$startId)")

        when (val action = intent?.action) {
            ACTION_BUMP_CHANNEL,
            ACTION_STOP_CHANNEL -> {
                val msg = serviceHandler.obtainMessage()
                msg.what = PeerCastController.MSG_PEERCAST_STATION_RPC
                msg.obj = MSG_OBJ_CALL_STOP_SELF
                msg.arg2 = startId

                val method = action.substringAfterLast(".")
                val channelId = intent.getStringExtra(EX_CHANNEL_ID)

                msg.data.putString(
                        EX_REQUEST,
                        JsonRpcUtil.createRequest(method, channelId)
                )

                serviceHandler.sendMessage(msg)
            }
            else -> {
                Timber.e("Illegal intent: $intent")
            }
        }
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

        if (appPrefs.isUPnPEnabled &&
                appPrefs.isUPnPCloseOnExit) {
            PecaPort.closePort(appPrefs.port)
        }

        stopForeground(true)
        nativeQuit()
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
    private fun notifyChannel(notifyType: Int, chId: String, jsonChannelInfo: String) {
        val chInfo = LibPeerCast.parseChannelInfo(jsonChannelInfo) ?: return

        when (notifyType) {
            NOTIFY_CHANNEL_START -> notificationHelper.start(chId, chInfo)
            NOTIFY_CHANNEL_UPDATE -> notificationHelper.update(chId, chInfo)
            NOTIFY_CHANNEL_STOP -> notificationHelper.remove(chId)
            else -> throw IllegalArgumentException()
        }

        Timber.d("$notifyType $chId $chInfo ${Thread.currentThread()}")
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
        private const val TAG = "PeerCastService"

        /**
         * arg2=startId
         */
        private val MSG_OBJ_CALL_STOP_SELF = Any()

        private const val NOTIFY_CHANNEL_START = 0
        private const val NOTIFY_CHANNEL_UPDATE = 1
        private const val NOTIFY_CHANNEL_STOP = 2

        // 通知ボタンAction
        const val ACTION_BUMP_CHANNEL = "org.peercast.core.ACTION.bumpChannel"
        const val ACTION_STOP_CHANNEL = "org.peercast.core.ACTION.stopChannel"

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


