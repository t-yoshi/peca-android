package org.peercast.core.lib.rpc

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 外部サイトの index.txt から取得されたチャンネル一覧
 * @author (c) 2021, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
@Parcelize
data class YpChannel internal constructor(
    val yellowPage: String,
    val name: String,
    val channelId: String,
    val tracker: String,
    val contactUrl: String,
    val genre: String,
    val description: String,
    val comment: String,
    val bitrate: Int,
    val contentType: String,
    val trackTitle: String,
    val album: String,
    val creator: String,
    val trackUrl: String,
    val listeners: Int,
    val relays: Int,
    // val uptime: Int
) : Parcelable

