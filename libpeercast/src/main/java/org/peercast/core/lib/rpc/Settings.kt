package org.peercast.core.lib.rpc

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * リレーに関する設定
 * @author (c) 2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
@Parcelize
@Serializable
data class Settings(
    val maxDirects: Int,
    val maxDirectsPerChannel: Int,
    val maxRelays: Int,
    val maxRelaysPerChannel: Int,
    val maxUpstreamRate: Int,
    /**get only*/
    val maxUpstreamRatePerChannel: Int,
) : Parcelable

