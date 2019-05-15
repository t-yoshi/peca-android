package org.peercast.core.lib

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager.NameNotFoundException
import android.os.*
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException

/**
 * PeerCast for Androidをコントロールする。
 *
 * @licenses Dual licensed under the MIT or GPL licenses.
 * @author (c) 2019, T Yoshizawa
 * @version 3.0.0
 */

class PeerCastController private constructor(private val appContext: Context) : RpcHostConnection {
    private var serverMessenger: Messenger? = null
    private val eventListeners = ArrayList<EventListener>()

    val isConnected: Boolean
        get() = serverMessenger != null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(arg0: ComponentName, binder: IBinder) {
            // Log.d(TAG, "onServiceConnected!");
            serverMessenger = Messenger(binder)
            eventListeners.forEach { it.onConnectService(this@PeerCastController) }
        }

        override fun onServiceDisconnected(arg0: ComponentName?) {
            // OSにKillされたとき。
            // Log.d(TAG, "onServiceDisconnected!");
            serverMessenger = null
            eventListeners.forEach { it.onDisconnectService(this@PeerCastController) }
        }
    }

    fun addEventListener(listener: EventListener) {
        if (listener in eventListeners)
            return
        if (isConnected)
            listener.onConnectService(this)
        eventListeners += listener
    }

    fun removeEventListener(listener: EventListener) {
        eventListeners -= listener
    }

    /**
     * 「PeerCast for Android」がインストールされているか調べる。
     *
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

        /**
         * unbindServiceを呼んだ後、もしくはOSによってサービスがKillされたときに呼ばれます。
         */
        fun onDisconnectService(controller: PeerCastController)
    }

    override suspend fun executeRpc(request: String): String = suspendCancellableCoroutine { co ->
        val cb = Handler.Callback { msg ->
            if (!co.isCancelled) {
                val ret = kotlin.runCatching {
                    msg.data.getString(EX_RESPONSE, null)
                            ?: throw NullPointerException("response is null")
                }
                co.resumeWith(ret)
            }
            true
        }

        val msg = Message.obtain(null, MSG_PEERCAST_STATION_RPC)

        msg.data.putString(EX_REQUEST, request)
        msg.replyTo = Messenger(Handler(Looper.getMainLooper(), cb))
        try {
            serverMessenger?.send(msg) ?: throw IllegalStateException("service not connected.")
        } catch (e: RemoteException) {
            if (!co.isCancelled) {
                co.resumeWithException(e)
            }
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
        )
    }

    /**
     * [Context.unbindService]を呼ぶ。 他からもbindされていなければPeerCastサービスは終了する。
     *
     * @return
     */
    fun unbindService() {
        if (!isConnected)
            return
        appContext.unbindService(serviceConnection)
        if (serverMessenger != null)
            serviceConnection.onServiceDisconnected(null)
    }

    companion object {
        const val MSG_GET_APPLICATION_PROPERTIES = 0x00

        //JsonRPC APIを使ってPeerCastに問い合わせる
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

        const val EX_REQUEST = "request"
        const val EX_RESPONSE = "response"


        private const val TAG = "PeCaCtrl"
        private const val PKG_PEERCAST = "org.peercast.core"
        private const val CLASS_NAME_PEERCAST_SERVICE = "$PKG_PEERCAST.PeerCastService"

        fun from(c: Context) = PeerCastController(c.applicationContext)

    }
}

