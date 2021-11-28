package org.peercast.core.common.upnp

interface PortMap {
    val protocol: String
    val externalPort: Int
    val internalPort: Int
    val internalClient: String
    val remoteHost: String
    val description: String
    val leaseDuration: Int
    val enabled: Boolean
}