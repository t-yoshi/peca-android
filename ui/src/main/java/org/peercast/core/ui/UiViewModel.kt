package org.peercast.core.ui

/**
 * (c) 2019, T Yoshizawa
 * Dual licensed under the MIT or GPLv3 licenses.
 */
import android.app.Application
import kotlinx.coroutines.flow.MutableStateFlow
import org.peercast.core.lib.PeerCastController
import org.peercast.core.lib.app.BasePeerCastViewModel
import org.peercast.core.lib.notify.NotifyChannelType
import org.peercast.core.lib.notify.NotifyMessageType
import org.peercast.core.lib.rpc.ChannelInfo
import timber.log.Timber
import java.util.*


class UiViewModel(a: Application) : BasePeerCastViewModel(a) {

    val notificationMessage = MutableStateFlow("")

    private val notifyEventListener = object : PeerCastController.NotifyEventListener {
        override fun onNotifyMessage(types: EnumSet<NotifyMessageType>, message: String) {
            Timber.d("$types $message")
            notificationMessage.value = message
        }

        override fun onNotifyChannel(
            type: NotifyChannelType,
            channelId: String,
            channelInfo: ChannelInfo,
        ) {
            Timber.d("$type $channelId $channelInfo")
        }
    }

    init {
        bindService(notifyEventListener)
    }

}
