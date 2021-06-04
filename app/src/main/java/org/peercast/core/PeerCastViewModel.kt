package org.peercast.core

/**
 * (c) 2019, T Yoshizawa
 * Dual licensed under the MIT or GPLv3 licenses.
 */
import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import org.peercast.core.lib.PeerCastController
import org.peercast.core.lib.PeerCastRpcClient
import org.peercast.core.lib.app.BasePeerCastViewModel
import org.peercast.core.lib.notify.NotifyMessageType
import org.peercast.core.lib.rpc.Channel
import org.peercast.core.lib.rpc.ChannelConnection
import timber.log.Timber
import java.io.IOException
import java.util.*

data class ActiveChannel(
    val ch: Channel,
    val connections: List<ChannelConnection>,
)

class PeerCastViewModel(
    private val a: Application,
    private val appPrefs: AppPreferences,
) : BasePeerCastViewModel(a) {

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

    override fun onConnectService(controller: PeerCastController) {
        super.onConnectService(controller)
        isServiceBoundLiveData.value = true
        activeChannelLiveData_.onActive()
    }

    override fun onNotifyMessage(types: EnumSet<NotifyMessageType>, message: String) {
        Timber.d("$types $message")
        notificationMessage.value = message
    }

    override fun onDisconnectService() {
        super.onDisconnectService()
        activeChannelLiveData_.onInactive()
        isServiceBoundLiveData.value = false
    }

    init {
        bindService()
    }

}
