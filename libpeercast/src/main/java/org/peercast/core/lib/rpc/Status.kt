package org.peercast.core.lib.rpc

import com.squareup.moshi.JsonClass
import org.peercast.core.lib.NullSafe
/**
 * (c) 2019, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */

/**
 * 稼働時間、ポート開放状態、IPアドレスなどの情報。
 *
 * @see PeerCastRpcClient.getStatus
 */
@JsonClass(generateAdapter = true)
data class Status internal constructor(
        val uptime: Int,
        @NullSafe val isFirewalled: Boolean,
        val globalRelayEndPoint: EndPoint?,
        val globalDirectEndPoint: EndPoint?,
        val localRelayEndPoint: EndPoint?,
        val localDirectEndPoint: EndPoint?
)

