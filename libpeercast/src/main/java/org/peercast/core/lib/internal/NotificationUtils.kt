package org.peercast.core.lib.internal

import org.peercast.core.lib.rpc.ChannelInfo

object NotificationUtils {

    /**
     * PeerCastService.notifyChannelで使用
     * */
    fun jsonToChannelInfo(json: String): ChannelInfo? {
        return SquareUtils.moshi.adapter(ChannelInfo::class.java).fromJson(json)
    }
}