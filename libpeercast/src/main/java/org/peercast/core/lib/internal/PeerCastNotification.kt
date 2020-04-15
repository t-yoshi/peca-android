package org.peercast.core.lib.internal

import android.os.*
import android.util.Log
import org.peercast.core.lib.BuildConfig
import org.peercast.core.lib.PeerCastController
import org.peercast.core.lib.notify.NotifyChannelType
import org.peercast.core.lib.notify.NotifyMessageType
import org.peercast.core.lib.rpc.ChannelInfo

object PeerCastNotification {

    /**(String)*/
    private const val EX_CHANNEL_ID = "channelId"

    /**(android.os.Messenger)*/
    const val EX_MESSENGER = "messenger"

    /**(String)*/
    private const val EX_MESSAGE = "message"

    /**(String)*/
    private const val EX_JSON_CHANNEL_INFO = "jsonChannelInfo"

    /**(Int)*/
    private const val EX_LIB_VERSION = "libVersion"

    private const val WHAT_NOTIFY_MESSAGE = 0x7144
    private const val WHAT_NOTIFY_CHANNEL = 0x7145

    fun Collection<Messenger>.sendNotifyMessage(type: Int, message: String, onRemoteException: (RemoteException) -> Unit) {
        val msg = Message.obtain()
        msg.what = WHAT_NOTIFY_MESSAGE
        msg.arg1 = type
        msg.data.run {
            putString(EX_MESSAGE, message)
            putInt(EX_LIB_VERSION, BuildConfig.VERSION_CODE)
        }
        forEach { messenger ->
            try {
                messenger.send(msg)
            } catch (e: RemoteException) {
                onRemoteException(e)
            }
        }
    }

    fun Collection<Messenger>.sendNotifyChannel(type: Int, chId: String, jsonChannelInfo: String, onRemoteException: (RemoteException) -> Unit) {
        val msg = Message.obtain()
        msg.what = WHAT_NOTIFY_CHANNEL
        msg.arg1 = type
        msg.data.run {
            putString(EX_CHANNEL_ID, chId)
            putString(EX_JSON_CHANNEL_INFO, jsonChannelInfo)
            putInt(EX_LIB_VERSION, BuildConfig.VERSION_CODE)
        }
        forEach { messenger ->
            try {
                messenger.send(msg)
            } catch (e: RemoteException) {
                onRemoteException(e)
            }
        }
    }

    private class ReceiveCallback(val listener: () -> PeerCastController.EventListener?) : Handler.Callback {
        override fun handleMessage(msg: Message): Boolean {
            val l = listener() ?: return false

            return when (msg.what) {
                WHAT_NOTIFY_MESSAGE -> {
                    l.onNotifyMessage(NotifyMessageType.from(msg.arg1), msg.data.getString(EX_MESSAGE)!!)
                    true
                }

                WHAT_NOTIFY_CHANNEL -> {
                    val chInfo = jsonToChannelInfo(msg.data.getString(EX_JSON_CHANNEL_INFO))
                            ?: return false
                    l.onNotifyChannel(
                            NotifyChannelType.values()[msg.arg1],
                            msg.data.getString(EX_CHANNEL_ID)!!, chInfo
                    )
                    true
                }

                else -> {
                    Log.e(TAG, "invalid msg.what=${msg.what}")
                    false
                }
            }
        }
    }

    /**
     * 通知を受信するMessengerを作成する
     * */
    internal fun createNotificationReceiveMessenger(listener: () -> PeerCastController.EventListener?): Messenger {
        return Messenger(Handler(Looper.getMainLooper(), ReceiveCallback(listener)))
    }

    /**
     * PeerCastService.notifyChannelで使用
     * */
    fun jsonToChannelInfo(json: String?): ChannelInfo? {
        json ?: return null
        return SquareUtils.moshi.adapter(ChannelInfo::class.java).fromJson(json)
    }

    private const val TAG = "PeCaNotify"

}