package org.peercast.core.ui

/**
 * (c) 2019, T Yoshizawa
 * Dual licensed under the MIT or GPLv3 licenses.
 */
import android.app.Application
import kotlinx.coroutines.flow.MutableStateFlow
import org.peercast.core.lib.app.BaseClientViewModel
import org.peercast.core.lib.notify.NotifyMessageType
import java.util.*


class UiViewModel(a: Application) : BaseClientViewModel(a) {

    val notificationMessage = MutableStateFlow("")

    override fun onNotifyMessage(types: EnumSet<NotifyMessageType>, message: String) {
        super.onNotifyMessage(types, message)
        //Timber.d("$types $message")
        notificationMessage.value = message
    }
}
