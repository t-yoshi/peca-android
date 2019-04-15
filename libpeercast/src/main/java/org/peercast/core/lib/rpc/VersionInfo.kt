package org.peercast.core.lib.rpc

import com.squareup.moshi.JsonClass

/**
 * (c) 2019, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
@JsonClass(generateAdapter = true)
data class VersionInfo internal constructor(
        val agentName: String,
        val apiVersion: String
)

