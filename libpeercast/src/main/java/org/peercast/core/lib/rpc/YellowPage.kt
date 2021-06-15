package org.peercast.core.lib.rpc

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.peercast.core.lib.internal.NullSafe
/**
 * @author (c) 2014-2021, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
@Parcelize
data class YellowPage internal constructor(
    val yellowPageId: Int,
    val name: String,
    val announceUri: String,
    @NullSafe val channelsUri: String,
    val protocol: String,
    val channels: List<ChannelStatus>,
) : Parcelable {

    /**配信中のチャンネルとルートサーバーとの接続状態*/
    @Parcelize
    data class ChannelStatus(
        val channelId: String,
        /**"Idle", "Connecting", "Connected", "Error" のいずれか*/
        val status: String,
    ) : Parcelable
}