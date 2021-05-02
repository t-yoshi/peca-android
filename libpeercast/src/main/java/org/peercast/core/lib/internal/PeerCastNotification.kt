package org.peercast.core.lib.internal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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

    /**(Int)*/
    private const val EX_NOTIFY_TYPE = "notifyType"

    /**(String)*/
    private const val EX_MESSAGE = "message"

    /**(String)*/
    private const val EX_JSON_CHANNEL_INFO = "jsonChannelInfo"

    /**(Int)*/
    private const val EX_LIB_VERSION = "libVersion"

    private const val ACT_NOTIFY_MESSAGE = "org.peercast.core.ACTION.notifyMessage"
    private const val ACT_NOTIFY_CHANNEL = "org.peercast.core.ACTION.notifyChannel"

    fun sendBroadCastNotifyMessage(c: Context, type: Int, message: String) {
        val i = Intent(ACT_NOTIFY_MESSAGE)
                .putExtra(EX_NOTIFY_TYPE, type)
                .putExtra(EX_MESSAGE, message)
                .putExtra(EX_LIB_VERSION, BuildConfig.LIB_VERSION)
        c.sendBroadcast(i)
    }

    fun sendBroadCastNotifyChannel(c: Context, type: Int, chId: String, jsonChannelInfo: String) {
        val i = Intent(ACT_NOTIFY_CHANNEL)
                .putExtra(EX_NOTIFY_TYPE, type)
                .putExtra(EX_CHANNEL_ID, chId)
                .putExtra(EX_JSON_CHANNEL_INFO, jsonChannelInfo)
                .putExtra(EX_LIB_VERSION, BuildConfig.LIB_VERSION)
        c.sendBroadcast(i)
    }

    private class NotificationBroadcastReceiver(val listener: () -> PeerCastController.EventListener?) : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val l = listener() ?: return

            val type = intent.getIntExtra(EX_NOTIFY_TYPE, 0)
            when(intent.action){
                ACT_NOTIFY_MESSAGE -> {
                    l.onNotifyMessage(NotifyMessageType.from(type), intent.getStringExtra(EX_MESSAGE).orEmpty())
                }

                ACT_NOTIFY_CHANNEL -> {
                    val chInfo = jsonToChannelInfo(intent.getStringExtra(EX_JSON_CHANNEL_INFO))
                            ?: return
                    l.onNotifyChannel(
                            NotifyChannelType.values()[type],
                            intent.getStringExtra(EX_CHANNEL_ID).orEmpty(), chInfo
                    )
                }

                else -> {
                    Log.e(TAG, "invalid action=${intent.action}")
                }
            }
        }
    }

    /**
     * 通知を受信するReceiverを登録する
     * */
    internal fun registerNotificationBroadcastReceiver(c: Context, listener: () -> PeerCastController.EventListener?): BroadcastReceiver {
        return NotificationBroadcastReceiver(listener).also { r->
            c.registerReceiver(r, IntentFilter().also { f->
                f.addAction(ACT_NOTIFY_MESSAGE)
                f.addAction(ACT_NOTIFY_CHANNEL)
            })
        }
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