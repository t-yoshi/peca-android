package org.peercast.core.lib.rpc

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.peercast.core.lib.rpc.io.MaybeNull

/**
 * 特定のチャンネルの情報。
 * @author (c) 2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
@Parcelize
@Serializable
data class ChannelStatus internal constructor(
    val status: ConnectionStatus,
    val source: String,
    val uptime: Int,

    @MaybeNull val localRelays: Int = 0,
    @MaybeNull val localDirects: Int = 0,
    @MaybeNull val totalRelays: Int = 0,
    @MaybeNull val totalDirects: Int = 0,
    @MaybeNull val isBroadcasting: Boolean = false,
    @MaybeNull val isRelayFull: Boolean = false,
    @MaybeNull val isDirectFull: Boolean = false,
    @MaybeNull val isReceiving: Boolean = false,
) : Parcelable
