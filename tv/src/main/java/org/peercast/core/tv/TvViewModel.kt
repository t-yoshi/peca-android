package org.peercast.core.tv

/**
 * (c) 2019, T Yoshizawa
 * Dual licensed under the MIT or GPLv3 licenses.
 */
import android.app.Application
import android.content.ActivityNotFoundException
import android.os.Handler
import android.os.SystemClock
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import org.peercast.core.lib.LibPeerCast
import org.peercast.core.lib.PeerCastController
import org.peercast.core.lib.app.BasePeerCastViewModel
import org.peercast.core.lib.notify.NotifyChannelType
import org.peercast.core.lib.notify.NotifyMessageType
import org.peercast.core.lib.rpc.ChannelInfo
import org.peercast.core.lib.rpc.YpChannel
import org.peercast.core.lib.toStreamIntent
import timber.log.Timber
import java.text.Normalizer
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap


class TvViewModel(
    private val a: Application,
    val prefs: TvPreferences,
) : BasePeerCastViewModel(a, false), PeerCastController.NotifyEventListener {

    init {
        bindService(this)
    }

    val ypChannelsFlow = MutableStateFlow<List<YpChannel>>(emptyList())


    private val messages = ArrayList<String>()
    private val handler = Handler()
    private var nextShow = 0L
    private val showToast = Runnable {
        //if (messages.isEmpty())
        //    return@Runnable
        val s = messages.joinToString(separator = "\n")
        messages.clear()
        Toast.makeText(a, s, Toast.LENGTH_SHORT).show()
        nextShow = SystemClock.elapsedRealtime() + 3000
    }

    override fun onNotifyMessage(types: EnumSet<NotifyMessageType>, message: String) {
        messages.add(message)
        val et = SystemClock.elapsedRealtime()
        handler.removeCallbacks(showToast)
        if (nextShow > et){
            handler.postDelayed(showToast, nextShow - et)
        } else {
            showToast.run()
        }
    }

    override fun onNotifyChannel(
        type: NotifyChannelType,
        channelId: String,
        channelInfo: ChannelInfo
    ) {

    }

    fun startPlayer(f: Fragment, ch: YpChannel){
        val i = ch.toStreamIntent(prefs.port)
        Timber.i("start player: ${i.data}")
        //i.setClass(requireContext(), PlaybackActivity::class.java)
        try {
            f.startActivity(i)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(a, "please install VLC Player", Toast.LENGTH_LONG).show()
            Timber.w(e)
        }
    }

}
