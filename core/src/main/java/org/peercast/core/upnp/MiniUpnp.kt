package org.peercast.core.upnp

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.peercast.core.BuildConfig
import timber.log.Timber
import java.io.IOException

internal class MiniUpnp {
    @Suppress("unused")
    private val nativeInstance: Long = 0

    init {
        initInstance()
    }

    /**@throws IOException */
    private external fun initInstance()

    external fun getIpAddress(): String

    /**@throws IOException */
    external fun addPort(port: Int, description: String, duration: Int)
/**/
    /**@throws IOException */
    external fun removePort(port: Int)

    /**@throws IOException */
    private external fun getPortMapsJson(): String

    /**@throws IOException */
    fun getPortMaps(): List<MiniUpnpPortMap> {
        val s = getPortMapsJson()
        return Json.decodeFromString(s)
    }

    private external fun getStatusesJson(): String

    fun getStatuses(): Map<String, String> {
        val s = getStatusesJson()
        return Json.decodeFromString(s)
    }

    protected external fun finalize()

    companion object {
        @JvmStatic
        private external fun initClass()

        init {
            System.loadLibrary("miniupnp")
            initClass()
        }
    }

}