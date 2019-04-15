package org.peercast.core.lib.rpc
/**
 * (c) 2019, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
data class ChannelInfo internal constructor(
        val name: String,
        val url: String,
        val genre: String,
        val desc: String,
        val comment: String,
        val bitrate: Int,
        val contentType: String,
        val mimeType: String
)


data class ChannelInfoResult internal constructor(
        val info: ChannelInfo,
        val track: Track)