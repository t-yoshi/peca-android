package org.peercast.core.lib.rpc

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class ChannelInfoResult internal constructor(
    val info: ChannelInfo,
    val track: Track,
) : Parcelable