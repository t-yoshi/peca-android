package org.peercast.core.tv

/**
 * (c) 2019, T Yoshizawa
 * Dual licensed under the MIT or GPLv3 licenses.
 */
import android.app.Application
import android.os.Handler
import android.os.SystemClock
import android.widget.Toast
import org.peercast.core.lib.app.BasePeerCastViewModel
import org.peercast.core.lib.notify.NotifyMessageType
import org.peercast.core.lib.rpc.YpChannel
import java.text.Normalizer
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap


class PeerCastTvViewModel(
    private val a: Application,
    private val appPrefs: TvPreferences,
) : BasePeerCastViewModel(a, false) {

    init {
        bindService()
    }

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
        super.onNotifyMessage(types, message)
        messages.add(message)
        val et = SystemClock.elapsedRealtime()
        handler.removeCallbacks(showToast)
        if (nextShow > et){
            handler.postDelayed(showToast, nextShow - et)
        } else {
            showToast.run()
        }
    }

    var ypChannels = emptyList<YpChannel>()
        set(value) {
            field = value

            normalizedText.clear()
            value.forEach {
                val s = Normalizer.normalize("${it.name} ${it.comment} ${it.genre} ${it.description}", Normalizer.Form.NFKD)
                normalizedText[it] = s
            }
        }
    private val normalizedText = LinkedHashMap<YpChannel, String>()

    fun searchChannel(text: String) : List<YpChannel> {
        val q = text.split("""\s+""".toRegex()).map { Normalizer.normalize(it, Normalizer.Form.NFKD) }
        return normalizedText.entries.filter { e->
            q.all { e.value.contains(it, true) }
        }.map { it.key }
    }

}
