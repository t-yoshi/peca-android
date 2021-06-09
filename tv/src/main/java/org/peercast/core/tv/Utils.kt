package org.peercast.core.tv

import org.peercast.core.lib.rpc.YpChannel
import org.unbescape.html.HtmlEscape

internal fun String.unescapeHtml(): String {
    return HtmlEscape.unescapeHtml(this)
}

internal val YpChannel.isNilId: Boolean
    get() = channelId == NIL_ID

internal val YpChannel.isNotNilId: Boolean
    get() = channelId != NIL_ID

/**00000000000000000000000000000000*/
internal const val NIL_ID = "00000000000000000000000000000000"