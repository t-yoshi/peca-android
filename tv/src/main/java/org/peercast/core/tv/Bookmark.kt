package org.peercast.core.tv

import android.content.Context
import androidx.core.content.edit
import org.peercast.core.lib.rpc.YpChannel

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

    fun all(): Set<String> {
        return prefs.all.keys.filter {
            it.startsWith(KEY_PREFIX_BOOKMARK)
        }.map {
            it.removePrefix(KEY_PREFIX_BOOKMARK)
        }.toSet()
    }

    companion object {
        private const val KEY_PREFIX_BOOKMARK = "bookmark-id:"
    }

}