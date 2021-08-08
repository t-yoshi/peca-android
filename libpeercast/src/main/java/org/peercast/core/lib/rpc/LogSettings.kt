package org.peercast.core.lib.rpc

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * ログレベル
 * @author (c) 2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
@Parcelize
@Serializable
data class LogSettings(val level: Int) : Parcelable