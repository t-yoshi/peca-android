package org.peercast.core.lib.rpc

import com.squareup.moshi.JsonClass

/**
 * (c) 2019, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
@JsonClass(generateAdapter = true)
data class Track internal constructor(
    val name: String,
    val genre: String,
    val album: String,
    val creator: String,
    val url: String
)

