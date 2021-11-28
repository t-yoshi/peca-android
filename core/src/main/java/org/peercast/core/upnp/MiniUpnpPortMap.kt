package org.peercast.core.upnp

import kotlinx.serialization.Serializable
import org.peercast.core.common.upnp.PortMap

@Serializable
internal data class MiniUpnpPortMap(
    override val externalPort: Int,
    override val internalClient: String,
    override val internalPort: Int,
    override val protocol: String,
    override val description: String,
    override val enabled: Boolean,
    override val remoteHost: String,
    override val leaseDuration: Int,
) : PortMap