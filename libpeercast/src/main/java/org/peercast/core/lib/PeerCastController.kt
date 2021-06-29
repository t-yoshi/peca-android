package org.peercast.core.lib

import android.content.*
import android.content.pm.PackageManager.NameNotFoundException
import android.os.*
import android.util.Log
import android.widget.Toast
import org.peercast.core.IPeerCastService
import org.peercast.core.lib.internal.PeerCastNotification
import org.peercast.core.lib.notify.NotifyChannelType
import org.peercast.core.lib.notify.NotifyMessageType
import org.peercast.core.lib.rpc.ChannelInfo
import java.io.IOException
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
    var eventListener: ConnectEventListener? = null
        set(value) {
            field = value
            if (isConnected)
                value?.onConnectService(this)
        }
    var notifyEventListener: NotifyEventListener? = null

    val isConnected: Boolean
        get() = service != null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(arg0: ComponentName, binder: IBinder) {
            Log.d(TAG, "onServiceConnected: interface=${binder.interfaceDescriptor}")
            if (binder.interfaceDescriptor == "org.peercast.core.IPeerCastService"){
                IPeerCastService.Stub.asInterface(binder)?.also {
                    service = it
                    eventListener?.onConnectService(this@PeerCastController)
                }
            } else {
                Toast.makeText(c, "Please update PeerCast app.", Toast.LENGTH_LONG).show()
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName?) {
            // OSにKillされたとき。
            Log.d(TAG, "onServiceDisconnected");
            service = null
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
                c.packageManager.getApplicationInfo(PKG_PEERCAST, 0)
                true
            } catch (e: NameNotFoundException) {
                false
            }
        }

    interface NotifyEventListener {
        /**通知を受信したとき*/
        fun onNotifyMessage(types: EnumSet<NotifyMessageType>, message: String)

        /**チャンネルの開始などの通知を受信したとき*/
        fun onNotifyChannel(type: NotifyChannelType, channelId: String, channelInfo: ChannelInfo)
    }


    interface ConnectEventListener {
        /**
         * bindService後にコネクションが確立されると呼ばれます。
         */
        fun onConnectService(controller: PeerCastController)

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
    val rpcEndPoint: String get() {
        val port = service?.port ?: throw IllegalStateException("service not connected.")
        return "http://127.0.0.1:$port/api/1"
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
        intent.putExtra(API_VERSION, BuildConfig.LIB_VERSION_CODE)

        return c.bindService(
                intent, serviceConnection,
                Context.BIND_AUTO_CREATE
        ).also {
            if (it && notificationReceiver == null){
                notificationReceiver =
                        PeerCastNotification.registerNotificationBroadcastReceiver(c){ notifyEventListener }
            }
        }
    }


    /**
     * [Context.unbindService]を呼ぶ。 他からもbindされていなければPeerCastサービスは終了する。
     */
    fun unbindService() {
        if (!isConnected)
            return

        notificationReceiver?.let(c::unregisterReceiver)
        notificationReceiver = null

        c.unbindService(serviceConnection)

        if (service != null)
            serviceConnection.onServiceDisconnected(null)
    }

    companion object {
        @Deprecated("Obsoleted v4.0")
        const val MSG_GET_APPLICATION_PROPERTIES = 0x00

        private const val TAG = "PeCaCtrl"
        private const val PKG_PEERCAST = "org.peercast.core"
        private const val CLASS_NAME_PEERCAST_SERVICE = "$PKG_PEERCAST.PeerCastService"
        private const val API_VERSION = "api-version"

        fun from(c: Context) = PeerCastController(c.applicationContext)

    }
}

