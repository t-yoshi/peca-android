package org.peercast.core.common

import java.util.*

/**
 * @author (c) 2014-2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
interface AppPreferences {
    /**動作ポート*/
    val port: Int

    /**UPnPを有効にし、サービス開始時にポートを開ける。*/
    var isUPnPEnabled: Boolean

    /**light, dark, system*/
    val htmlTheme: String
}

