package org.peercast.core.ui.tv

/**
 * (c) 2019, T Yoshizawa
 * Dual licensed under the MIT or GPLv3 licenses.
 */
import android.app.Application
import androidx.core.text.HtmlCompat
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import org.peercast.core.common.AppPreferences
import org.peercast.core.common.PeerCastConfig
import org.peercast.core.lib.app.BaseClientViewModel
import org.peercast.core.lib.notify.NotifyChannelType
import org.peercast.core.lib.notify.NotifyMessageType
import org.peercast.core.lib.rpc.ChannelInfo
import org.peercast.core.ui.tv.yp.BookmarkManager
import org.peercast.core.ui.tv.yp.YpChannelsFlow
import java.util.EnumSet


class TvViewModel(
    a: Application,
    val prefs: AppPreferences,
    val config: PeerCastConfig,
    val ypChannels: YpChannelsFlow,
) : BaseClientViewModel(a) {

    val bookmark = BookmarkManager(a)
    val toastMessageFlow: SharedFlow<CharSequence> = MutableSharedFlow()

    override fun onNotifyMessage(types: EnumSet<NotifyMessageType>, message: String) {
        super.onNotifyMessage(types, message)
        sendInfoToast(message)
    }

    fun sendInfoToast(s: CharSequence) {
        viewModelScope.launch {
            (toastMessageFlow as MutableSharedFlow).emit(s)
        }
    }

    fun sendErrorToast(t: Throwable) {
        sendErrorToast(t.message ?: "$t")
    }

    fun sendErrorToast(s: String) {
        sendInfoToast(
            HtmlCompat.fromHtml("<font color=red>$s", 0)
        )
    }

    override fun onNotifyChannel(
        type: NotifyChannelType,
        channelId: String,
        channelInfo: ChannelInfo,
    ) {

    }

}
