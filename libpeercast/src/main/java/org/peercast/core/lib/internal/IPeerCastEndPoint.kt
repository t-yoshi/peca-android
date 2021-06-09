package org.peercast.core.lib.internal

/**RPC接続へのURL*/
internal interface IPeerCastEndPoint {
    suspend fun getRpcEndPoint() : String
}