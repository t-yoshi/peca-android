package org.peercast.core.lib.rpc

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * ログバッファーの内容
 * @author (c) 2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
@Parcelize
data class Log internal constructor(val from: Int,
               val lines: Int,
               val log: List<String>) : Parcelable