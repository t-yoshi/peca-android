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
import kotlinx.coroutines.flow.collect
import org.peercast.core.lib.PeerCastController
import org.peercast.core.lib.PeerCastRpcClient
import org.peercast.core.lib.app.BasePeerCastViewModel
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
    val connections: List<ChannelConnection>,
)

class AppViewModel(
    private val a: Application,
    private val appPrefs: AppPreferences,
) : BasePeerCastViewModel(a) {

    val statusLiveData = MutableLiveData<CharSequence>(a.getString(R.string.t_stopped))
    val version: String =
        a.getString(R.string.app_version, BuildConfig.VERSION_NAME, BuildConfig.YT_VERSION)
    val notificationMessage = MutableLiveData<String>()



    suspend fun getActiveChannels() : List<ActiveChannel> {
        val client = rpcClientFlow.value
        if (client == null){
            Timber.w("service is not connected.")
            return emptyList()
        }
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

        return channels.map { ch ->
            ActiveChannel(ch, connections.getValue(ch).filter {
                it.type !in listOf("direct", "source")
            })
        }
    }


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

    @Deprecated("")
    fun executeRpcCommand(
        scope: CoroutineScope = viewModelScope,
        f: suspend (PeerCastRpcClient) -> Unit,
    ) = scope.launch {
        val client = rpcClientFlow.value
        if (client == null) {
            Timber.e("client is null")
            return@launch
        }
        runCatching {
            f(client)
        }.onFailure {
            Timber.e(it)
        }
    }

    init {
        bindService(notifyEventListener)
    }

}
