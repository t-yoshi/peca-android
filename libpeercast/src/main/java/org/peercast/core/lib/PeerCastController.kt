package org.peercast.core.lib

import android.content.*
import android.content.pm.PackageManager.NameNotFoundException
import android.os.*
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.delay
import org.peercast.core.INotificationCallback
import org.peercast.core.IPeerCastService
import org.peercast.core.lib.internal.NotificationUtils
import org.peercast.core.lib.internal.ServiceIntents
import org.peercast.core.lib.notify.NotifyChannelType
import org.peercast.core.lib.notify.NotifyMessageType
import org.peercast.core.lib.rpc.ChannelInfo
import java.util.*

/**
 * PeerCast for Androidをコントロールする。
 *
 * @licenses Dual licensed under the MIT or GPL licenses.
 * @author (c) 2019-2021, T Yoshizawa
 * @version 4.0.0
 */
class PeerCastController private constructor(private val c: Context) {
    private var service: IPeerCastService? = null
    var eventListener: EventListener? = null
        set(value) {
            field = value
            if (isConnected)
                value?.onConnectService(this)
        }

    val isConnected: Boolean
        get() = service != null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(arg0: ComponentName, binder: IBinder) {
            Log.d(TAG, "onServiceConnected: interface=${binder.interfaceDescriptor}")
            if (binder.interfaceDescriptor == "org.peercast.core.IPeerCastService") {
                IPeerCastService.Stub.asInterface(binder)?.also { s ->
                    service = s
                    kotlin.runCatching {
                        s.registerNotificationCallback(notificationCallback)
                    }.onFailure { Log.w(TAG, it) }
                    eventListener?.onConnectService(this@PeerCastController)
                }
            } else {
                Toast.makeText(c, "Please update PeerCast app.", Toast.LENGTH_LONG).show()
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName?) {
            // OSにKillされたとき。
            Log.d(TAG, "onServiceDisconnected")
            kotlin.runCatching {
                service?.unregisterNotificationCallback(notificationCallback)
            }.onFailure { Log.w(TAG, it) }

            service = null
            eventListener?.onDisconnectService()
        }
    }

    private val notificationCallback = object : INotificationCallback.Stub() {
        val handler = Handler(Looper.getMainLooper())
        override fun onNotifyChannel(notifyType: Int, chId: String, jsonChannelInfo: String) {
            handler.post {
                eventListener?.onNotifyChannel(
                    NotifyChannelType.values()[notifyType],
                    chId, NotificationUtils.jsonToChannelInfo(jsonChannelInfo) ?: return@post
                )
            }
        }

        override fun onNotifyMessage(types: Int, message: String) {
            handler.post {
                eventListener?.onNotifyMessage(
                    NotifyMessageType.from(types), message
                )
            }
        }
    }

    /**
     * 「PeerCast for Android」がインストールされているか調べる。
     * @return "org.peercast.core" がインストールされていればtrue。
     */
    val isInstalled: Boolean
        get() {
            return try {
                c.packageManager.getApplicationInfo(ServiceIntents.PKG_PEERCAST, 0)
                true
            } catch (e: NameNotFoundException) {
                false
            }
        }

    interface EventListener {
        /**
         * bindService後にコネクションが確立されたとき。
         */
        fun onConnectService(controller: PeerCastController)

        /**
         * unbindServiceを呼んだ後、もしくはOSによってサービスがKillされたとき。
         */
        fun onDisconnectService()

        /**通知を受信したとき*/
        fun onNotifyMessage(types: EnumSet<NotifyMessageType>, message: String)

        /**チャンネルの開始などの通知を受信したとき*/
        fun onNotifyChannel(type: NotifyChannelType, channelId: String, channelInfo: ChannelInfo)
    }

    /**
     * JSON-RPCへのエンドポイントを返す。
     * 例: "http://127.0.0.1:7144/api/1"
     * @throws IllegalStateException サービスにbindされていない
     * @throws RemoteException 取得できないとき
     * */
    val rpcEndPoint: String
        get() {
            val port = service?.port ?: error("service not connected.")
            return "http://127.0.0.1:$port/api/1"
        }

    /**
     * [Context.bindService]を呼び、PeerCastのサービスを開始する。
     */
    @Deprecated("use tryBindService()")
    fun bindService(): Boolean {
        if (!isInstalled) {
            Log.e(TAG, "PeerCast not installed.")
            return false
        }

        return c.bindService(
            ServiceIntents.SERVICE4_INTENT, serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    /**
     * [Context#bindService]を呼び、PeerCastサービスへの接続を試みる。
     * ノート:
     * OSのカスタマイズ状況によっては、電池やセキリティー設定をいじっていて、
     * バックグラウンドからのサービス起動を禁止している場合がある。なので、
     * 一度Activity経由でフォアグラウンドでのサービス起動を試みる。
     */
    suspend fun tryBindService(): Boolean {
        if (!isInstalled) {
            Log.e(TAG, "PeerCast not installed.")
            return false
        }

        for (i in 0..2) {
            val r = c.bindService(
                ServiceIntents.SERVICE4_INTENT, serviceConnection,
                Context.BIND_AUTO_CREATE
            )
            if (r) {
                return true
            }
            if (i == 0) {
                try {
                    c.startActivity(ServiceIntents.SERVICE_LAUNCHER_INTENT)
                } catch (e: RuntimeException) {
                    Log.e(TAG, "startActivity failed:", e)
                }
            }
            delay(2_000)
        }
        return false
    }

    /**
     * [Context.unbindService]を呼ぶ。 他からもbindされていなければPeerCastサービスは終了する。
     */
    fun unbindService() {
        if (!isConnected)
            return

        c.unbindService(serviceConnection)

        serviceConnection.onServiceDisconnected(null)
    }

    companion object {
        @Deprecated("Obsoleted since v4.0")
        const val MSG_GET_APPLICATION_PROPERTIES = 0x00

        private const val TAG = "PeCaCtrl"

        fun from(c: Context) = PeerCastController(c.applicationContext)

    }
}

