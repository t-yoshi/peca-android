package org.peercast.core.lib

import android.content.*
import android.content.pm.PackageManager.NameNotFoundException
import android.os.*
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import org.peercast.core.lib.internal.IPeerCastEndPoint
import org.peercast.core.lib.internal.PeerCastNotification
import org.peercast.core.lib.notify.NotifyChannelType
import org.peercast.core.lib.notify.NotifyMessageType
import org.peercast.core.lib.rpc.ChannelInfo
import java.io.IOException
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * PeerCast for Androidをコントロールする。
 *
 * @licenses Dual licensed under the MIT or GPL licenses.
 * @author (c) 2019-2020, T Yoshizawa
 * @version 3.1.0
 */

class PeerCastController private constructor(private val appContext: Context) : IPeerCastEndPoint {
    private var serverMessenger: Messenger? = null
    var eventListener: EventListener? = null
        set(value) {
            field = value
            if (isConnected)
                value?.onConnectService(this)
        }

    val isConnected: Boolean
        get() = serverMessenger != null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(arg0: ComponentName, binder: IBinder) {
            // Log.d(TAG, "onServiceConnected!");
            serverMessenger = Messenger(binder)
            eventListener?.onConnectService(this@PeerCastController)
        }

        override fun onServiceDisconnected(arg0: ComponentName?) {
            // OSにKillされたとき。
            // Log.d(TAG, "onServiceDisconnected!");
            serverMessenger = null
            eventListener?.onDisconnectService()
        }
    }

    private var notificationReceiver : BroadcastReceiver? = null

    /**
     * 「PeerCast for Android」がインストールされているか調べる。
     * @return "org.peercast.core" がインストールされていればtrue。
     */
    val isInstalled: Boolean
        get() {
            return try {
                appContext.packageManager.getApplicationInfo(PKG_PEERCAST, 0)
                true
            } catch (e: NameNotFoundException) {
                false
            }
        }

    interface EventListener {
        /**
         * bindService後にコネクションが確立されると呼ばれます。
         */
        fun onConnectService(controller: PeerCastController)

        /**通知を受信したとき*/
        fun onNotifyMessage(types: EnumSet<NotifyMessageType>, message: String)

        /**チャンネルの開始などの通知を受信したとき*/
        fun onNotifyChannel(type: NotifyChannelType, channelId: String, channelInfo: ChannelInfo)

        /**
         * unbindServiceを呼んだ後、もしくはOSによってサービスがKillされたときに呼ばれます。
         */
        fun onDisconnectService()
    }

    /**
     * JSON-RPCへのエンドポイントを返す。
     * 例: "http://127.0.0.1:7144/api/1"
     * @throws IllegalStateException サービスにbindされていない
     * @throws IOException 取得できないとき
     * */
    override suspend fun getRpcEndPoint(): String = suspendCancellableCoroutine { co ->
        val messenger = serverMessenger
        if (messenger == null) {
            co.resumeWithException(IllegalStateException("service not connected."))
            return@suspendCancellableCoroutine
        }

        val cb = Handler.Callback { msg ->
            if (!co.isCancelled) {
                val port = msg.data.getInt("port", 0)
                co.resume("http://127.0.0.1:$port/api/1")
            }
            true
        }

        val msg = Message.obtain(null, MSG_GET_APPLICATION_PROPERTIES)
        msg.replyTo = Messenger(Handler(Looper.getMainLooper(), cb))
        try {
            messenger.send(msg)
        } catch (e: RemoteException) {
            //Log.e(TAG,"RemoteException occurred", e)
            co.resumeWithException(IOException(e))
        }
    }

    /**
     * [Context.bindService]を呼び、PeerCastのサービスを開始する。
     */
    fun bindService(): Boolean {
        if (!isInstalled) {
            Log.e(TAG, "PeerCast not installed.")
            return false
        }
        val intent = Intent(CLASS_NAME_PEERCAST_SERVICE)
        // NOTE: LOLLIPOPからsetPackage()必須
        intent.setPackage(PKG_PEERCAST)

        return appContext.bindService(
                intent, serviceConnection,
                Context.BIND_AUTO_CREATE
        ).also {
            if (it){
                notificationReceiver =
                        PeerCastNotification.registerNotificationBroadcastReceiver(appContext){ eventListener }
            }
        }
    }


    /**
     * [Context.unbindService]を呼ぶ。 他からもbindされていなければPeerCastサービスは終了する。
     */
    fun unbindService() {
        if (!isConnected)
            return

        notificationReceiver?.let(appContext::unregisterReceiver)
        notificationReceiver = null

        appContext.unbindService(serviceConnection)

        if (serverMessenger != null)
            serviceConnection.onServiceDisconnected(null)
    }

    companion object {
        const val MSG_GET_APPLICATION_PROPERTIES = 0x00

        //JsonRPC APIを使ってPeerCastに問い合わせる
        //v3.1で廃止
        @Deprecated("Obsoleted v3.1")
        const val MSG_PEERCAST_STATION_RPC = 0x10000

        //v3.0で廃止
        @Deprecated("Obsoleted v3.0")
        const val MSG_GET_CHANNELS = 0x01

        @Deprecated("Obsoleted v3.0")
        const val MSG_GET_STATS = 0x02

        @Deprecated("Obsoleted v3.0")
        const val MSG_CMD_CHANNEL_BUMP = 0x10

        @Deprecated("Obsoleted v3.0")
        const val MSG_CMD_CHANNEL_DISCONNECT = 0x11

        @Deprecated("Obsoleted v3.0")
        const val MSG_CMD_CHANNEL_KEEP_YES = 0x12

        @Deprecated("Obsoleted v3.0")
        const val MSG_CMD_CHANNEL_KEEP_NO = 0x13

        @Deprecated("Obsoleted v3.0")
        const val MSG_CMD_SERVENT_DISCONNECT = 0x20

        @Deprecated("Obsoleted v3.1")
        const val EX_REQUEST = "request"

        @Deprecated("Obsoleted v3.1")
        const val EX_RESPONSE = "response"


        private const val TAG = "PeCaCtrl"
        private const val PKG_PEERCAST = "org.peercast.core"
        private const val CLASS_NAME_PEERCAST_SERVICE = "$PKG_PEERCAST.PeerCastService"

        fun from(c: Context) = PeerCastController(c.applicationContext)

    }
}

