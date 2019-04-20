package org.peercast.core.lib.rpc
/**
 * チャンネルの接続情報
 * @author (c) 2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
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
