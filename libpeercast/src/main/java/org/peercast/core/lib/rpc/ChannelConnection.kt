package org.peercast.core.lib.rpc

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.peercast.core.lib.rpc.io.MaybeNull

/**
 * チャンネルの接続情報
 * @author (c) 2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
@Parcelize
@Serializable
data class ChannelConnection internal constructor(
    val connectionId: Int,
    val type: String,
    val status: String,
    val sendRate: Float,
    val recvRate: Float,
    val protocolName: String,
    @MaybeNull val localRelays: Int = 0,
    @MaybeNull val localDirects: Int = 0,
    @MaybeNull val contentPosition: Long = 0L,
    val agentName: String?,
    val remoteEndPoint: EndPoint?,
    //val remoteHostStatus: List<String>,
    val remoteName: String?,
) : Parcelable

