package org.peercast.core.ui

/**
 * (c) 2019, T Yoshizawa
 * Dual licensed under the MIT or GPLv3 licenses.
 */
import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import org.peercast.core.lib.app.BaseClientViewModel
import org.peercast.core.lib.notify.NotifyMessageType
import java.util.*


class UiViewModel(a: Application) : BaseClientViewModel(a) {

    val notificationMessage = MutableStateFlow("")

    val scrollable = MutableStateFlow(true)
    val title = MutableStateFlow("")
    val expandAppBar = MutableSharedFlow<Boolean>()

    override fun onNotifyMessage(types: EnumSet<NotifyMessageType>, message: String) {
        super.onNotifyMessage(types, message)
        //Timber.d("$types $message")
        notificationMessage.value = message
    }
}
