package org.peercast.core.lib.rpc
/**
 * チャンネルの情報
 * @author (c) 2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
data class Channel internal constructor(
        val channelId: String,
        val status: ChannelStatus,
        val info: ChannelInfo,
        val track: Track
)

