package org.peercast.core.ui.tv.util

/**
 * @author (c) 2014-2021, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import org.peercast.core.lib.rpc.YpChannel
import org.unbescape.html.HtmlEscape

internal fun startFragment(fm: FragmentManager, f: Fragment) {
    fm.beginTransaction()
        .addToBackStack(null)
        .replace(android.R.id.content, f)
        .commit()
}

internal fun Fragment.finishFragment() {
    val fm = parentFragmentManager
    fm.beginTransaction()
        .remove(this)
        .commitAllowingStateLoss()
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
