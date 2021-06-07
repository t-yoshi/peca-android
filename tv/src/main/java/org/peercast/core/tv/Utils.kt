package org.peercast.core.tv

import org.unbescape.html.HtmlEscape

internal fun String.unescapeHtml(): String {
    return HtmlEscape.unescapeHtml(this)
}

