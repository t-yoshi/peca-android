package org.peercast.core.tv

/**
 * (c) 2019, T Yoshizawa
 * Dual licensed under the MIT or GPLv3 licenses.
 */
import android.app.Application
import android.os.Handler
import android.os.SystemClock
import android.widget.Toast
import kotlinx.coroutines.flow.MutableStateFlow
import org.peercast.core.lib.PeerCastController
import org.peercast.core.lib.app.BasePeerCastViewModel
import org.peercast.core.lib.notify.NotifyChannelType
import org.peercast.core.lib.notify.NotifyMessageType
import org.peercast.core.lib.rpc.ChannelInfo
import org.peercast.core.lib.rpc.YpChannel
import org.peercast.core.common.AppPreferences
import org.peercast.core.tv.yp.Bookmark
import org.peercast.core.tv.yp.YpChannelsFlow
import java.util.*
import kotlin.collections.ArrayList


class TvViewModel(
    private val a: Application,
    val prefs: AppPreferences,
    val ypChannels: YpChannelsFlow,
    val bookmark: Bookmark,
) : BasePeerCastViewModel(a, false), PeerCastController.NotifyEventListener {

    init {
        bindService(this)
    }

    private val messages = ArrayList<String>()
    private val handler = Handler()
    private var nextShow = 0L
    private val runShowToast = Runnable {
        //if (messages.isEmpty())
        //    return@Runnable
        val s = messages.joinToString(separator = "\n")
        messages.clear()
        showInfoToast(s, Toast.LENGTH_SHORT)
        nextShow = SystemClock.elapsedRealtime() + 3000
    }

    override fun onNotifyMessage(types: EnumSet<NotifyMessageType>, message: String) {
        messages.add(message)
        val et = SystemClock.elapsedRealtime()
        handler.removeCallbacks(runShowToast)
        if (nextShow > et) {
            handler.postDelayed(runShowToast, nextShow - et)
        } else {
            runShowToast.run()
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
