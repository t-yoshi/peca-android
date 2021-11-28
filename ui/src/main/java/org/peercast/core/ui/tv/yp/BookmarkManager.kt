package org.peercast.core.ui.tv.yp

/**
 * @author (c) 2014-2021, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
import android.content.Context
import androidx.core.content.edit
import org.peercast.core.lib.rpc.YpChannel
import org.peercast.core.ui.tv.util.isNilId
import kotlin.math.min

class BookmarkManager(c: Context) {
    private val prefs = c.getSharedPreferences("tv-bookmark", Context.MODE_PRIVATE)

    operator fun plusAssign(ch: YpChannel) {
        if (ch.isNilId || contains(ch))
            return

        prefs.edit {
            putLong(KEY_PREFIX_BOOKMARK_NAME + ch.name, 1)
        }
    }

    operator fun minusAssign(ch: YpChannel) {
        prefs.edit {
            remove(KEY_PREFIX_BOOKMARK_NAME + ch.name)
        }
    }

    operator fun contains(ch: YpChannel): Boolean {
        return prefs.getLong(KEY_PREFIX_BOOKMARK_NAME + ch.name, 0) > 0
    }

    fun toggle(ch: YpChannel) {
        if (contains(ch)) {
            minusAssign(ch)
        } else {
            plusAssign(ch)
        }
    }

    fun incrementPlayedCount(ch: YpChannel) {
        val n = prefs.getLong(KEY_PREFIX_BOOKMARK_NAME + ch.name, 0)
        if (n > 0) {
            prefs.edit {
                putLong(KEY_PREFIX_BOOKMARK_NAME + ch.name, min(n + 1, Long.MAX_VALUE))
            }
        }
    }

    /**ブックマーク済みを先頭にもってくる*/
    fun comparator(): (YpChannel, YpChannel) -> Int {
        // Map<name,num_played>
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
        private const val KEY_PREFIX_BOOKMARK_NAME = "bookmark-name:" //value=再生回数(long)
    }

}