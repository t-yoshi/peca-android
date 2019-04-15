package org.peercast.core.lib.rpc

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SourceStream(
        val name: String,
        val desc: String,
        val scheme: String,
        val type: Int,
        val defaultUri: String
)
