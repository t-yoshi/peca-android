package org.peercast.core

/**
 * (c) 2019, T Yoshizawa
 * Dual licensed under the MIT or GPLv3 licenses.
 */
import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.peercast.core.lib.PeerCastController
import org.peercast.core.lib.PeerCastRpcClient
import org.peercast.core.lib.rpc.Channel
import org.peercast.core.lib.rpc.ChannelConnection
import org.peercast.core.lib.rpc.JsonRpcException
import timber.log.Timber

data class Channel2(
        val ch: Channel,
        val connections: List<ChannelConnection>
)

class PeerCastViewModel(private val a: Application,
                        private val pecaController: PeerCastController,
                        private val appPrefs: AppPreferences)
    : AndroidViewModel(a), PeerCastController.EventListener {

    var rpcClient: PeerCastRpcClient? = null
        private set

    private val handler = Handler(Looper.getMainLooper())

    val status = MutableLiveData<CharSequence>(a.getString(R.string.t_stopped))


    private val channels_ = object : MutableLiveData<List<Channel2>>(emptyList()), Runnable {
        public override fun onActive() {
            handler.post(this)
        }

        public override fun onInactive() {
            handler.removeCallbacks(this)
        }

        override fun run() {
            if (!hasActiveObservers())
                return

            viewModelScope.launch {
                try {
                    rpcClient?.let { cl ->
                        val channels = cl.getChannels()
                        val connections = channels.map { ch ->
                            ch to cl.getChannelConnections(ch.channelId)
                        }.toMap()

//                        channels.forEach { ch->
//                            val relayTree = cl.getChannelRelayTree(ch.channelId)
//                            Timber.d("relay=$relayTree")
//                        }


                        val relayConnections = connections.values.flatten().filter { it.type != "direct" }
                        val recvRate = relayConnections.map { it.recvRate }.sum()
                        val sendRate = relayConnections.map { it.sendRate }.sum()
                        status.postValue(a.getString(
                                R.string.status_format,
                                recvRate / 1000 * 8,
                                sendRate / 1000 * 8,
                                appPrefs.port))

                        value = channels.map { ch ->
                            Channel2(ch, connections.getValue(ch).filter {
                                it.type !in listOf("direct", "source")
                            })
                        }
                        Timber.d("--> $value")
                    }
                } catch (e: JsonRpcException) {
                    Timber.e(e)
                }
            }
            handler.postDelayed(this, 8_000)
        }
    }

    val channels: LiveData<List<Channel2>> get() = channels_


    init {
        pecaController.addEventListener(this)
        pecaController.bindService()
    }

    override fun onConnectService(controller: PeerCastController) {
        rpcClient = PeerCastRpcClient(controller)
        channels_.onActive()
    }

    override fun onDisconnectService(controller: PeerCastController) {
        channels_.onInactive()
        rpcClient = null
    }

    override fun onCleared() {
        pecaController.unbindService()
        pecaController.removeEventListener(this)
    }
}