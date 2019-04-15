package org.peercast.core.lib.rpc

import com.squareup.moshi.JsonClass
import org.peercast.core.lib.NullSafe

@JsonClass(generateAdapter = true)
data class ChannelConnection internal constructor(
        val connectionId: Int,
        val type: String,
        val status: String,
        val sendRate: Float,
        val recvRate: Float,
        val protocolName: String,
        @NullSafe val localRelays: Int,
        @NullSafe val localDirects: Int,
        @NullSafe val contentPosition: Long,
        val agentName: String?,
        val remoteEndPoint: EndPoint?,
    //val remoteHostStatus: List<String>,
        val remoteName: String?
)

