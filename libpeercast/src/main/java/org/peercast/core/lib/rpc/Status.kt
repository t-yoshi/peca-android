package org.peercast.core.lib.rpc

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * 稼働時間、ポート開放状態、IPアドレスなどの情報。
 * @author (c) 2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 * @see PeerCastRpcClient.getStatus
 */
@Parcelize
@Serializable
data class Status internal constructor(
        val uptime: Int,
        val isFirewalled: Boolean = false,
        val globalRelayEndPoint: EndPoint?,
        val globalDirectEndPoint: EndPoint?,
        val localRelayEndPoint: EndPoint?,
        val localDirectEndPoint: EndPoint?,
) : Parcelable

