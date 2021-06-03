package org.peercast.core.lib.rpc

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

/**
 * (c) 2019, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
@Parcelize
@JsonClass(generateAdapter = true)
data class Track internal constructor(
    val name: String,
    val genre: String,
    val album: String,
    val creator: String,
    val url: String
) : Parcelable

