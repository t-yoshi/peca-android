package org.peercast.core.lib.internal

import org.peercast.core.lib.rpc.ChannelInfo

/**
 * 内部用のUtilクラス
 * (c) 2019, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
object JsonRpcUtil {

    /**
     * PeerCastService.notifyChannelで使用
     * */
    fun jsonToChannelInfo(json: String): ChannelInfo? {
        return SquareUtils.moshi.adapter(ChannelInfo::class.java).fromJson(json)
    }

}