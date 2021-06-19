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
            putLong(KEY_PREFIX_BOOKMARK_NAME + ypChannel.name, 1)
        }
    }

    fun remove(ypChannel: YpChannel) {
        prefs.edit {
            remove(KEY_PREFIX_BOOKMARK_NAME + ypChannel.name)
        }
    }

    fun exists(ypChannel: YpChannel): Boolean {
        return prefs.getLong(KEY_PREFIX_BOOKMARK_NAME + ypChannel.name, 0) > 0
    }

    fun toggle(ypChannel: YpChannel) {
        if (exists(ypChannel)) {
            remove(ypChannel)
        } else {
            add(ypChannel)
        }
    }

    fun incrementPlayedCount(ypChannel: YpChannel) {
        val n = prefs.getLong(KEY_PREFIX_BOOKMARK_NAME + ypChannel.name, 0)
        if (n > 0) {
            prefs.edit {
                putLong(KEY_PREFIX_BOOKMARK_NAME + ypChannel.name, n + 1)
            }
        }
    }

    /**ブックマーク済みを先頭にもってくる*/
    fun comparator(): (YpChannel, YpChannel) -> Int {
        // Map<(name),(num played)>
        val m = prefs.all.keys.filter {
            it.startsWith(KEY_PREFIX_BOOKMARK_NAME)
        }.map {
            it.removePrefix(KEY_PREFIX_BOOKMARK_NAME) to prefs.getLong(it, 0)
        }.toMap()

        return { c1, c2 ->
            (m[c2.name] ?: 0).compareTo(m[c1.name] ?: 0)
        }
    }

    companion object {
        //private const val KEY_PREFIX_BOOKMARK_ID = "bookmark-id:"
        private const val KEY_PREFIX_BOOKMARK_NAME = "bookmark-name:" //value=再生回数(long)
    }

}