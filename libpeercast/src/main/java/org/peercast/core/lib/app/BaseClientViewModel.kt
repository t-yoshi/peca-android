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

abstract class BaseClientViewModel(
    a: Application,
) : AndroidViewModel(a), PeerCastController.EventListener {

    private val controller = PeerCastController.from(a)

    private val _rpcClient = MutableStateFlow<PeerCastRpcClient?>(null)

    /**サービスに接続されたとき、PeerCastRpcClientを返す。切断時はnull。*/
    val rpcClient: StateFlow<PeerCastRpcClient?> = _rpcClient

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
        channelInfo: ChannelInfo,
    ) {
        Log.d(TAG, "$type $channelId $channelInfo")
    }

    @CallSuper
    override fun onConnectService(controller: PeerCastController) {
        _rpcClient.value = PeerCastRpcClient(controller)
    }

    @CallSuper
    override fun onDisconnectService() {
        _rpcClient.value = null
    }

    @CallSuper
    override fun onNotifyMessage(types: EnumSet<NotifyMessageType>, message: String) {
        //ポート設定が変更されたとき
        //@see PeerCastService.cpp
        //@see servhs.cpp
        if (controller.isConnected && message == "設定を保存しました。"
            && NotifyMessageType.PeerCast in types
        ) {
            _rpcClient.value = PeerCastRpcClient(controller)
        }
    }

    @CallSuper
    override fun onCleared() {
        super.onCleared()
        bindJob?.cancel()
        controller.unbindService()
    }

    companion object {
        private const val TAG = "BasePeerCastViewModel"
    }
}

