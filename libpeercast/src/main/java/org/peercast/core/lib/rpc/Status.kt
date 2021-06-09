package org.peercast.core.lib.rpc

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import org.peercast.core.lib.internal.NullSafe

/**
 * 稼働時間、ポート開放状態、IPアドレスなどの情報。
 * @author (c) 2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 * @see PeerCastRpcClient.getStatus
 */
@Parcelize
@JsonClass(generateAdapter = true)
data class Status internal constructor(
        val uptime: Int,
        @NullSafe val isFirewalled: Boolean,
        val globalRelayEndPoint: EndPoint?,
        val globalDirectEndPoint: EndPoint?,
        val localRelayEndPoint: EndPoint?,
        val localDirectEndPoint: EndPoint?
): Parcelable

