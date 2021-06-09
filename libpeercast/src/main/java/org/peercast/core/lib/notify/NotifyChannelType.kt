package org.peercast.core.lib.notify

enum class NotifyChannelType {
    Start,
    Update,
    Stop;

    val nativeValue get() = ordinal
}