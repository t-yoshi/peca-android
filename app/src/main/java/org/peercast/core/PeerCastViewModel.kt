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

class PeerCastServiceResult(val channels: List<Channel>, val stats: Stats, val props: Properties)

class PeerCastViewModel(a: Application,
                        private val pecaController: PeerCastController)
    : AndroidViewModel(a), PeerCastController.EventListener {

    private val handler = Handler(Looper.getMainLooper())

    private val serviceResultLiveData_ = object : MutableLiveData<PeerCastServiceResult>(), Runnable {
        public override fun onActive() {
            handler.post(this)
        }

        public override fun onInactive() {
            handler.removeCallbacks(this)
        }

        override fun run() {
            if (!pecaController.isConnected || !hasActiveObservers())
                return

            viewModelScope.launch {
                value = pecaController.let {
                    PeerCastServiceResult(
                            it.getChannels(),
                            it.getStats(),
                            it.getsProperties()
                    )
                }
            }

            handler.postDelayed(this, 8_000)
        }
    }

    val serviceResultLiveData: LiveData<PeerCastServiceResult> get() = serviceResultLiveData_


    init {
        pecaController.addEventListener(this)
        pecaController.bindService()
    }

    override fun onConnectService(controller: PeerCastController) {
        serviceResultLiveData_.onActive()
    }

    override fun onDisconnectService(controller: PeerCastController) {
        serviceResultLiveData_.onInactive()
    }

    override fun onCleared() {
        pecaController.unbindService()
        pecaController.removeEventListener(this)
    }
}