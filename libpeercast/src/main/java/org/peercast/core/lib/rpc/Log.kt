package org.peercast.core.lib.rpc
/**
 * ログバッファーの内容
 * @author (c) 2019, T Yoshizawa
 * @licenses Dual licensed under the MIT or GPL licenses.
 */
data class Log internal constructor(val from: Int,
               val lines: Int,
               val log: List<String>)