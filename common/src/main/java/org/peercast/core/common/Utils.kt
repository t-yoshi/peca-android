package org.peercast.core.common

/**
 * @author (c) 2014-2021, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import java.util.*

/**@see https://qiita.com/hirano/items/10da4d1b9c86218dd50a*/
val Context.isFireTv: Boolean
    get() = packageManager.hasSystemFeature("amazon.hardware.fire_tv")

val Context.isTvMode: Boolean
    get() {
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }
