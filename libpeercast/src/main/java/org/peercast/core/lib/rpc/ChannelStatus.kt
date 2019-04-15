package org.peercast.core.lib.rpc

import com.squareup.moshi.JsonClass
import org.peercast.core.lib.NullSafe

/**
 * (c) 2019, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
@JsonClass(generateAdapter = true)
data class ChannelStatus internal constructor(
        val status: ConnectionStatus,
        val source: String,
        val uptime: Int,

        @NullSafe val localRelays: Int,
        @NullSafe val localDirects: Int,
        @NullSafe val totalRelays: Int,
        @NullSafe val totalDirects: Int,
        @NullSafe val isBroadcasting: Boolean,
        @NullSafe val isRelayFull: Boolean,
        @NullSafe val isDirectFull: Boolean,
        @NullSafe val isReceiving: Boolean
)
