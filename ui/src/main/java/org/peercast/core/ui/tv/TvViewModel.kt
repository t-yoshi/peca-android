package org.peercast.core.ui.tv

/**
 * (c) 2019, T Yoshizawa
 * Dual licensed under the MIT or GPLv3 licenses.
 */
import android.app.Application
import android.text.Html
import android.text.SpannableString
import androidx.core.text.HtmlCompat
import kotlinx.coroutines.flow.*
import org.peercast.core.common.AppPreferences
import org.peercast.core.lib.app.BaseClientViewModel
import org.peercast.core.lib.notify.NotifyChannelType
import org.peercast.core.lib.notify.NotifyMessageType
import org.peercast.core.lib.rpc.ChannelInfo
import org.peercast.core.ui.tv.yp.BookmarkManager
import org.peercast.core.ui.tv.yp.YpChannelsFlow
import java.util.*


class TvViewModel(
    a: Application,
    val prefs: AppPreferences,
    val ypChannels: YpChannelsFlow,
) : BaseClientViewModel(a) {

    val bookmark = BookmarkManager(a)
    private val messageFlow = MutableStateFlow<CharSequence>("")
    val toastMessageFlow : Flow<CharSequence> = messageFlow.filter { it.isNotBlank() }


    override fun onNotifyMessage(types: EnumSet<NotifyMessageType>, message: String) {
        super.onNotifyMessage(types, message)
        sendInfoToast(message)
    }

    fun sendInfoToast(s: CharSequence){
        messageFlow.value = s
    }

    fun sendErrorToast(t: Throwable){
        sendErrorToast(t.message ?: "$t")
    }

    fun sendErrorToast(s: String){
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
