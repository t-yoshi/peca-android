package org.peercast.core.lib.rpc

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

/**
 * バージョン情報
 * @author (c) 2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
@Parcelize
@JsonClass(generateAdapter = true)
data class VersionInfo internal constructor(
        val agentName: String,
        val apiVersion: String
) : Parcelable

