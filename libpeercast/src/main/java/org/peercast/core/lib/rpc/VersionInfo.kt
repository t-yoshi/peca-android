package org.peercast.core.lib.rpc

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * バージョン情報
 * @author (c) 2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
@Parcelize
@Serializable
data class VersionInfo internal constructor(
    val agentName: String,
    val apiVersion: String,
) : Parcelable

