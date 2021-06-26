package org.peercast.core.tv
/**
 * @author (c) 2014-2021, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.fragment.app.Fragment
import org.peercast.core.lib.rpc.YpChannel
import org.unbescape.html.HtmlEscape

internal fun Fragment.finishFragment() {
    parentFragmentManager.beginTransaction()
        .remove(this)
        .commit()
}

internal fun String.unescapeHtml(): String {
    return HtmlEscape.unescapeHtml(this)
}

internal val YpChannel.isNilId: Boolean
    get() = channelId == NIL_ID

internal val YpChannel.isNotNilId: Boolean
    get() = channelId != NIL_ID

/**00000000000000000000000000000000*/
internal const val NIL_ID = "00000000000000000000000000000000"
