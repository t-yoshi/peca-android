package org.peercast.core.common.upnp

interface UpnpManager {
    /**@throws java.io.IOException*/
    suspend fun addPort(port: Int)

    /**@throws java.io.IOException*/
    suspend fun removePort(port: Int)

    /**@throws java.io.IOException*/
    suspend fun getPortMaps(): List<PortMap>

    suspend fun getStatuses(): Map<String, String>
}