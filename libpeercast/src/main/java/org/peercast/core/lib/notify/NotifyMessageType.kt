package org.peercast.core.lib.notify

import java.util.*

enum class NotifyMessageType(val nativeValue: Int) {
    Upgrade(0x0001),
    PeerCast(0x0002),
    Broadcasters(0x0004),
    TrackInfo(0x0008);

    companion object {
        internal fun from(value: Int): EnumSet<NotifyMessageType> {
            return EnumSet.noneOf(NotifyMessageType::class.java).also { es ->
                values().forEach { v ->
                    if ((value and v.nativeValue) != 0)
                        es.add(v)
                }
            }
        }
    }
}
