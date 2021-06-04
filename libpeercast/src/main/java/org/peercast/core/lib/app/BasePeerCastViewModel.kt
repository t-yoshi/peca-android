package org.peercast.core.lib.app

import android.app.Application
import android.util.Log
import androidx.annotation.CallSuper
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.*
import org.peercast.core.lib.PeerCastController
import org.peercast.core.lib.PeerCastRpcClient
import org.peercast.core.lib.notify.NotifyChannelType
import org.peercast.core.lib.notify.NotifyMessageType
import org.peercast.core.lib.rpc.ChannelInfo
import java.util.*
import kotlin.coroutines.CoroutineContext

open class BasePeerCastViewModel(
    a: Application,
    private val unbindOnCleared: Boolean = true,
) : AndroidViewModel(a), PeerCastController.EventListener, CoroutineScope {

    protected val controller = PeerCastController.from(a)
    protected var rpcClient: PeerCastRpcClient? = null
        private set
    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    fun executeRpcCommand(
        scope: CoroutineScope = this,
        timeout: Int = 5000,
        onError: suspend (e: Throwable) -> Unit = { Log.e(TAG, it.message, it) },
        f: suspend (PeerCastRpcClient) -> Unit,
    ) = scope.launch {
        var client = rpcClient
        var t = timeout
        while (client == null && t > 0) {
            client = rpcClient
            delay(10)
            t -= 10
        }
        if (client == null) {
            onError(IllegalStateException("rpcClient is null"))
            return@launch
        }
        runCatching {
            f(client)
        }.onFailure {
            onError(it)
        }
    }

    @CallSuper
    override fun onConnectService(controller: PeerCastController) {
        rpcClient = PeerCastRpcClient(controller)
    }

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

    @CallSuper
    override fun onDisconnectService() {
        job.cancel()
        rpcClient = null
    }

    protected fun bindService() {
        job = Job()
        controller.eventListener = this
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
    }
}

