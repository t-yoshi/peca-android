package org.peercast.core.lib.rpc

import com.squareup.moshi.JsonClass
/**
 * 対応しているソースストリームプロトコルの情報
 * @author (c) 2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
@JsonClass(generateAdapter = true)
data class SourceStream(
        val name: String,
        val desc: String,
        val scheme: String,
        val type: Int,
        val defaultUri: String
)
