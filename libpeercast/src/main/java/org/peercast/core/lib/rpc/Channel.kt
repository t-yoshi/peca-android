package org.peercast.core.lib.rpc

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * チャンネルの情報
 * @author (c) 2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
@Parcelize
@Serializable
data class Channel internal constructor(
    val channelId: String,
    val status: ChannelStatus,
    val info: ChannelInfo,
    val track: Track,
) : Parcelable

