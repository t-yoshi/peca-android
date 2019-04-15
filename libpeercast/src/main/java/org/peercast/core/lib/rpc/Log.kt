package org.peercast.core.lib.rpc

data class Log internal constructor(val from: Int,
               val lines: Int,
               val log: List<String>)