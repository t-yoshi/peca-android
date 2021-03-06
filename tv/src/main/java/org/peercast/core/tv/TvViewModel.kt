package org.peercast.core.tv

/**
 * (c) 2019, T Yoshizawa
 * Dual licensed under the MIT or GPLv3 licenses.
 */
import android.app.Application
import android.os.SystemClock
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.peercast.core.common.AppPreferences
import org.peercast.core.lib.app.BaseClientViewModel
import org.peercast.core.lib.notify.NotifyChannelType
import org.peercast.core.lib.notify.NotifyMessageType
import org.peercast.core.lib.rpc.ChannelInfo
import org.peercast.core.tv.yp.Bookmark
import org.peercast.core.tv.yp.YpChannelsFlow
import java.util.*
import kotlin.collections.ArrayList


class TvViewModel(
    private val a: Application,
    val prefs: AppPreferences,
    val ypChannels: YpChannelsFlow,
    val bookmark: Bookmark,
) : BaseClientViewModel(a) {

    private val messages = ArrayList<String>()
    private var nextShow = 0L
    private var tj: Job? = null

    override fun onNotifyMessage(types: EnumSet<NotifyMessageType>, message: String) {
        super.onNotifyMessage(types, message)

        messages.add(message)
        if (tj?.isActive == true)
            return
        tj = viewModelScope.launch {
            while (messages.isNotEmpty()) {
                delay(nextShow - SystemClock.elapsedRealtime())
                val s = messages.let {
                    //一度に3つ表示する
                    val l = it.take(3)
                    it.subList(0, l.size).clear()
                    l.joinToString(separator = "\n")
                }
                showInfoToast(s, Toast.LENGTH_SHORT)
                nextShow = SystemClock.elapsedRealtime() + 4000
            }
        }
    }

    override fun onNotifyChannel(
        type: NotifyChannelType,
        channelId: String,
        channelInfo: ChannelInfo,
    ) {

    }


    fun showInfoToast(text: CharSequence, duration: Int = Toast.LENGTH_LONG) {
        Toast.makeText(a, text, duration).show()
    }

}
