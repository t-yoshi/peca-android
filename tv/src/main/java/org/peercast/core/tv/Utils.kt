package org.peercast.core.tv
/**
 * @author (c) 2014-2021, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
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

/**@see https://qiita.com/hirano/items/10da4d1b9c86218dd50a*/
val Context.isFireTv : Boolean
    get() = packageManager.hasSystemFeature("amazon.hardware.fire_tv")

val Context.isTvMode : Boolean
    get() {
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }