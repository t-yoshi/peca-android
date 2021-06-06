package org.peercast.core.lib.app

import android.app.Application
import android.util.Log
import androidx.annotation.CallSuper
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.peercast.core.lib.PeerCastController
import org.peercast.core.lib.PeerCastRpcClient
import org.peercast.core.lib.notify.NotifyChannelType
import org.peercast.core.lib.notify.NotifyMessageType
import org.peercast.core.lib.rpc.ChannelInfo
import java.util.*

abstract class BasePeerCastViewModel(
    a: Application,
    private val unbindOnCleared: Boolean = true,
) : AndroidViewModel(a) {

    private val controller = PeerCastController.from(a)

    /**サービスに接続されたとき、PeerCastRpcClientを返す。切断時はnull。*/
    val rpcClientFlow: StateFlow<PeerCastRpcClient?> = MutableStateFlow(null)

    private val connectEventListener = object : PeerCastController.ConnectEventListener {
        override fun onConnectService(controller: PeerCastController) {
            (rpcClientFlow as MutableStateFlow).value = PeerCastRpcClient(controller)
        }

        @CallSuper
        override fun onDisconnectService() {
            (rpcClientFlow as MutableStateFlow).value = null
        }
    }

    protected fun bindService(notifyEventListener: PeerCastController.NotifyEventListener? = null) {
        controller.eventListener = connectEventListener
        controller.notifyEventListener = notifyEventListener ?: sDebugNotifyEventListener
        controller.bindService()
    }

    @CallSuper
    override fun onCleared() {
        super.onCleared()
        if (unbindOnCleared)
            controller.unbindService()
    }


    companion object {
        private const val TAG = "BasePeerCastViewModel"

        private val sDebugNotifyEventListener = object : PeerCastController.NotifyEventListener {
            override fun onNotifyChannel(
                type: NotifyChannelType,
                channelId: String,
                channelInfo: ChannelInfo,
            ) {
                Log.d(TAG, "$type $channelId $channelInfo")
            }

            override fun onNotifyMessage(types: EnumSet<NotifyMessageType>, message: String) {
                Log.d(TAG, "$types $message")
            }
        }
    }
}

