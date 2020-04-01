package org.peercast.core.lib.rpc

import com.squareup.moshi.JsonClass

/**
 * リレーに関する設定
 * @author (c) 2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
@JsonClass(generateAdapter = true)
data class Settings(
    val maxDirects: Int,
    val maxDirectsPerChannel: Int,
    val maxRelays: Int,
    val maxRelaysPerChannel: Int,
    val maxUpstreamRate: Int,
    /**get only*/
    val maxUpstreamRatePerChannel: Int
)

