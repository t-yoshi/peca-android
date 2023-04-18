package org.peercast.core.lib.internal

import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.peercast.core.lib.rpc.ChannelInfo

object NotificationUtils {

    /**
     * PeerCastService.notifyChannelで使用
     * */
    fun jsonToChannelInfo(json: String): ChannelInfo? {
        val format = Json {
            isLenient = true
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
        return try {
            format.decodeFromString<ChannelInfo>(json)
        } catch (e: SerializationException) {
            null
        }
    }
}