package org.peercast.core.lib.rpc

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * 対応しているソースストリームプロトコルの情報
 * @author (c) 2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
@Parcelize
@Serializable
data class SourceStream(
    val name: String,
    val desc: String,
    val scheme: String,
    val type: Int,
    val defaultUri: String,
) : Parcelable
