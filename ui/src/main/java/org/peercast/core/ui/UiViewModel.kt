package org.peercast.core.ui

/**
 * (c) 2019, T Yoshizawa
 * Dual licensed under the MIT or GPLv3 licenses.
 */
import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.peercast.core.lib.app.BaseClientViewModel
import org.peercast.core.lib.notify.NotifyMessageType
import timber.log.Timber
import java.util.*


class UiViewModel(a: Application) : BaseClientViewModel(a) {

    val notificationMessage = MutableSharedFlow<String>()

    init {
        viewModelScope.launch {
            bindService()
        }
    }

    override fun onNotifyMessage(types: EnumSet<NotifyMessageType>, message: String) {
        Timber.d("$types $message")
        viewModelScope.launch {
            notificationMessage.emit(message)
        }
    }
}
