package org.peercast.core

/**
 * (c) 2019, T Yoshizawa
 * Dual licensed under the MIT or GPLv3 licenses.
 */
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import org.peercast.core.lib.PeerCastController
import org.peercast.core.lib.PeerCastRpcClient
import org.peercast.core.lib.notify.NotifyChannelType
import org.peercast.core.lib.notify.NotifyMessageType
import org.peercast.core.lib.rpc.Channel
import org.peercast.core.lib.rpc.ChannelConnection
import org.peercast.core.lib.rpc.ChannelInfo
import timber.log.Timber
import java.io.IOException
import java.util.*

data class ActiveChannel(
    val ch: Channel,
    val connections: List<ChannelConnection>
)

class PeerCastViewModel(
    private val a: Application,
    private val appPrefs: AppPreferences
) : AndroidViewModel(a) {

    private val pecaController = PeerCastController.from(a)

    private var rpcClient: PeerCastRpcClient? = null

    val statusLiveData = MutableLiveData<CharSequence>(a.getString(R.string.t_stopped))
    val isServiceBoundLiveData = MutableLiveData<Boolean>()
    val version: String =
        a.getString(R.string.app_version, BuildConfig.VERSION_NAME, BuildConfig.YT_VERSION)
    val notificationMessage = MutableLiveData<String>()

    private val activeChannelLiveData_ =
        object : MutableLiveData<List<ActiveChannel>>(emptyList()) {
            private var j: Job? = null

            public override fun onActive() {
                j?.cancel()
                j = viewModelScope.launch(Dispatchers.Default) {
                    val client = rpcClient
                    var err = 0
                    while (isActive && hasActiveObservers() && client != null && err < 3) {
                        try {
                            requestRpc(client)
                        } catch (e: IOException) {
                            Timber.w(e)
                            err++
                        }
                        delay(8_000)
                    }
                }
            }

            public override fun onInactive() {
                j?.cancel()
                j = null
            }

            private suspend fun requestRpc(client: PeerCastRpcClient) {
                val channels = client.getChannels()
                val connections = channels.map { ch ->
                    ch to client.getChannelConnections(ch.channelId)
                }.toMap()
                val relayConnections = connections.values.flatten().filter { it.type != "direct" }
                val recvRate = relayConnections.map { it.recvRate }.sum()
                val sendRate = relayConnections.map { it.sendRate }.sum()

                statusLiveData.postValue(
                    a.getString(
                        R.string.status_format,
                        recvRate / 1000 * 8,
                        sendRate / 1000 * 8,
                        appPrefs.port
                    )
                )

                postValue(channels.map { ch ->
                    ActiveChannel(ch, connections.getValue(ch).filter {
                        it.type !in listOf("direct", "source")
                    })
                })
            }
        }

    val activeChannelLiveData: LiveData<List<ActiveChannel>> get() = activeChannelLiveData_

    fun executeRpcCommand(
        scope: CoroutineScope = viewModelScope,
        f: suspend (PeerCastRpcClient) -> Unit
    ) = scope.launch {
        var client = rpcClient
        var timeout = 5000
        while (client == null && timeout > 0) {
            client = rpcClient
            delay(10)
            timeout -= 10
        }
        if (client == null) {
            Timber.w("rpcClient is null")
            return@launch
        }
        runCatching {
            f(client)
        }.onFailure {
            Timber.e(it)
        }
    }

    private val peerCastEventListener = object : PeerCastController.EventListener {
        override fun onConnectService(controller: PeerCastController) {
            rpcClient = PeerCastRpcClient(controller)
            isServiceBoundLiveData.value = true
            activeChannelLiveData_.onActive()
        }

        override fun onNotifyChannel(
            type: NotifyChannelType,
            channelId: String,
            channelInfo: ChannelInfo
        ) {
            Timber.d("$type $channelId $channelInfo")
        }

        override fun onNotifyMessage(types: EnumSet<NotifyMessageType>, message: String) {
            Timber.d("$types $message")
            notificationMessage.value = message
        }

        override fun onDisconnectService() {
            activeChannelLiveData_.onInactive()
            isServiceBoundLiveData.value = false
            rpcClient = null
        }
    }

    init {
        pecaController.eventListener = peerCastEventListener
        pecaController.bindService()
    }

    override fun onCleared() {
        super.onCleared()
        pecaController.unbindService()
    }
}
