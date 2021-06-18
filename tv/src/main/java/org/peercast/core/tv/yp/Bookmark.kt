package org.peercast.core.tv.yp
/**
 * @author (c) 2014-2021, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
import android.content.Context
import androidx.core.content.edit
import org.peercast.core.lib.rpc.YpChannel
import org.peercast.core.tv.isNilId

class Bookmark(c: Context) {
    private val prefs = c.getSharedPreferences("tv-bookmark", Context.MODE_PRIVATE)

    fun add(ypChannel: YpChannel) {
        if (ypChannel.isNilId)
            return

        prefs.edit {
            putLong(KEY_PREFIX_BOOKMARK + ypChannel.channelId, System.currentTimeMillis())
        }
    }

    fun remove(ypChannel: YpChannel) {
        prefs.edit {
            remove(KEY_PREFIX_BOOKMARK + ypChannel.channelId)
        }
    }

    fun exists(ypChannel: YpChannel): Boolean {
        return prefs.getLong(KEY_PREFIX_BOOKMARK + ypChannel.channelId, 0) > 0
    }

    fun toggle(ypChannel: YpChannel) {
        if (exists(ypChannel)) {
            remove(ypChannel)
        } else {
            add(ypChannel)
        }
    }

    /**ブックマーク済みを先頭にもってくる*/
    fun comparator(): (YpChannel, YpChannel) -> Int {
        val all = prefs.all.keys.filter {
            it.startsWith(KEY_PREFIX_BOOKMARK)
        }.map {
            it.removePrefix(KEY_PREFIX_BOOKMARK)
        }.toSet()

        return { c1, c2 ->
            (c2.channelId in all).compareTo(c1.channelId in all)
        }
    }

    companion object {
        private const val KEY_PREFIX_BOOKMARK = "bookmark-id:"
    }

}