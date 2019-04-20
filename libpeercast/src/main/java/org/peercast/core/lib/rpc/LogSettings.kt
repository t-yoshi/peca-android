package org.peercast.core.lib.rpc

import com.squareup.moshi.JsonClass
/**
 * ログレベル
 * @author (c) 2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
@JsonClass(generateAdapter = true)
data class LogSettings(val level: Int)