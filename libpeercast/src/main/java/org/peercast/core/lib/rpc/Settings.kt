package org.peercast.core.lib.rpc

import com.squareup.moshi.JsonClass

/**
 * (c) 2019, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
@JsonClass(generateAdapter = true)
data class Settings(
    var maxDirects: Int,
    var maxDirectsPerChannel: Int,
    var maxRelays: Int,
    var maxRelaysPerChannel: Int,
    var maxUpstreamRate: Int,
    /**get only*/
    val maxUpstreamRatePerChannel: Int
)

