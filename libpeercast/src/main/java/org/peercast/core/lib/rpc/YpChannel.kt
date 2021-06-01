package org.peercast.core.lib.rpc

import org.peercast.core.lib.internal.NullSafe

/**
 * 外部サイトの index.txt から取得されたチャンネル一覧
 * @author (c) 2021, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
data class YpChannel internal constructor(
    @NullSafe val yellowPage: String,
    @NullSafe val name: String,
    @NullSafe val channelId: String,
    @NullSafe val tracker: String,
    @NullSafe val contactUrl: String,
    @NullSafe val genre: String,
    @NullSafe val description: String,
    @NullSafe val comment: String,
    @NullSafe val bitrate: Int,
    @NullSafe val contentType: String,
    @NullSafe val trackTitle: String,
    @NullSafe val album: String,
    @NullSafe val creator: String,
    @NullSafe val trackUrl: String,
    @NullSafe val listeners: Int,
    @NullSafe val relays: Int,
   // @NullSafe val uptime: Int
)

