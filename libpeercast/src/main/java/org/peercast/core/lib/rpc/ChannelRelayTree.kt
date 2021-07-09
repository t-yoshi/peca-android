package org.peercast.core.lib.rpc

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.peercast.core.lib.rpc.io.MaybeNull

/**
 * リレーツリー情報
 * @author (c) 2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
@Parcelize
@Serializable
data class ChannelRelayTree(
    val sessionId: String,
    val address: String,
    val port: Int,
    val isFirewalled: Boolean,
    val localRelays: Int,
    val localDirects: Int,

    @MaybeNull val isTracker: Boolean = false,
    @MaybeNull val isRelayFull: Boolean = false,
    @MaybeNull val isDirectFull: Boolean = false,
    @MaybeNull val isReceiving: Boolean = false,
    @MaybeNull val isControlFull: Boolean = false,
//    val version: String?,
//    val versionVP: String?,
//    val versionEX: String?,
    val children: List<ChannelRelayTree>,
) : Parcelable

