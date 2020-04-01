package org.peercast.core.lib.rpc

import com.squareup.moshi.JsonClass
import org.peercast.core.lib.internal.NullSafe

/**
 * リレーツリー情報
 * @author (c) 2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
@JsonClass(generateAdapter = true)
data class ChannelRelayTree (
        val sessionId: String,
        val address: String,
        val port: Int,
        val isFirewalled: Boolean,
        val localRelays: Int,
        val localDirects: Int,

        @NullSafe val isTracker: Boolean,
        @NullSafe val isRelayFull: Boolean,
        @NullSafe val isDirectFull: Boolean,
        @NullSafe val isReceiving: Boolean,
        @NullSafe val isControlFull: Boolean,
//    val version: String?,
//    val versionVP: String?,
//    val versionEX: String?,
        val children: List<ChannelRelayTree>
)

