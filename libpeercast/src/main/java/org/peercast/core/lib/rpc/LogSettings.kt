package org.peercast.core.lib.rpc

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LogSettings(val level: Int)