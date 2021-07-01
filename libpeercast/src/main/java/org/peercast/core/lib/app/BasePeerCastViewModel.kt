package org.peercast.core.lib.app

import android.app.Application
import android.util.Log
import androidx.annotation.CallSuper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.peercast.core.lib.PeerCastController
import org.peercast.core.lib.PeerCastRpcClient
import org.peercast.core.lib.notify.NotifyChannelType
import org.peercast.core.lib.notify.NotifyMessageType
import org.peercast.core.lib.rpc.ChannelInfo
import java.util.*

abstract class BasePeerCastViewModel(
    a: Application,
) : AndroidViewModel(a), PeerCastController.EventListener {

    private val controller = PeerCastController.from(a)

    /**サービスに接続されたとき、PeerCastRpcClientを返す。切断時はnull。*/
    val rpcClient: StateFlow<PeerCastRpcClient?> = MutableStateFlow(null)

    private var bindJob: Job? = null

    open fun bindService() {
        if (bindJob?.isActive == true)
            return
        controller.eventListener = this
        bindJob = viewModelScope.launch {
            controller.tryBindService()
        }
    }

    override fun onNotifyChannel(
        type: NotifyChannelType,
        channelId: String,
        channelInfo: ChannelInfo
    ) {
        Log.d(TAG, "$type $channelId $channelInfo")
    }

    @CallSuper
    override fun onConnectService(controller: PeerCastController) {
        (rpcClient as MutableStateFlow).value = PeerCastRpcClient(controller)
    }

    @CallSuper
    override fun onDisconnectService() {
        (rpcClient as MutableStateFlow).value = null
    }

    override fun onNotifyMessage(types: EnumSet<NotifyMessageType>, message: String) {
        Log.d(TAG, "$types $message")
    }

    override fun onCleared() {
        super.onCleared()
        bindJob?.cancel()
        controller.unbindService()
    }

    companion object {
        private const val TAG = "BasePeerCastViewModel"
    }
}

