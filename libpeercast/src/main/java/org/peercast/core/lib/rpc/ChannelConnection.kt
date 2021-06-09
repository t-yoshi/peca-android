package org.peercast.core.lib.rpc

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import org.peercast.core.lib.internal.NullSafe

/**
 * チャンネルの接続情報
 * @author (c) 2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
@Parcelize
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
) : Parcelable

