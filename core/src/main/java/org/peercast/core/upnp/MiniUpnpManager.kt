package org.peercast.core.upnp

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import org.peercast.core.common.upnp.PortMap
import org.peercast.core.common.upnp.UpnpManager
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.hours


class MiniUpnpManager(private val a: Application) : UpnpManager {

    private val singleThreadExecutor = Executors.newSingleThreadScheduledExecutor()
    private val singleDispatcher = singleThreadExecutor.asCoroutineDispatcher()

    private var resMiniUpnp: Result<MiniUpnp>? = null

    init {
        val connMan = a.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connMan.registerNetworkCallback(
            REQ_TYPE_WIFI_ETHERNET,
            object : ConnectivityManager.NetworkCallback() {
                private fun clearClient() {
                    singleThreadExecutor.execute {
                        resMiniUpnp = null
                        Timber.d("resMiniUpnp was cleared.")
                    }
                }

                override fun onAvailable(network: Network) {
                    clearClient()
                }

                override fun onLost(network: Network) {
                    clearClient()
                }
            })
    }

    private fun getOrCreateMiniUpnp(): MiniUpnp {
        return resMiniUpnp?.getOrThrow() ?: kotlin.runCatching {
            MiniUpnp()
        }.let { r ->
            resMiniUpnp = r
            r.onFailure {
                singleThreadExecutor.schedule(Runnable {
                    if (resMiniUpnp?.isSuccess != true)
                        resMiniUpnp = null
                }, RETRY_DISCOVERY_SECONDS, TimeUnit.SECONDS)
            }.getOrThrow()
        }
    }

    override suspend fun addPort(port: Int) {
        withContext(singleDispatcher) {
            getOrCreateMiniUpnp().let { mu ->
                try {
                    mu.getPortMaps().firstOrNull { m ->
                        m.description == ADD_PORT_DESCRIPTION && (
                                m.internalPort != port ||
                                        m.internalClient != mu.getIpAddress() ||
                                        (m.leaseDuration != 0 && m.leaseDuration < ADD_PORT_DURATION / 3)
                                )
                    }?.let {
                        mu.removePort(port)
                    }
                } catch (e: IOException) {
                    Timber.w(e, "Couldn't remove exists port $port.")
                }
                mu.addPort(port, ADD_PORT_DESCRIPTION, ADD_PORT_DURATION)
            }
        }
    }

    override suspend fun removePort(port: Int) {
        withContext(singleDispatcher) {
            getOrCreateMiniUpnp().removePort(port)
        }
    }

    override suspend fun getPortMaps(): List<PortMap> {
        return withContext(singleDispatcher) {
            getOrCreateMiniUpnp().getPortMaps()
        }
    }

    override suspend fun getStatuses(): Map<String, String> {
        return withContext(singleDispatcher) {
            getOrCreateMiniUpnp().getStatuses().mapKeys {
                // R.string.upnp_ip_address -> "Ip Address"
                getStringResourceByName(it.key)
            }
        }
    }

    private fun getStringResourceByName(name: String): String {
        val resId = a.resources.getIdentifier(
            name, "string", a.packageName
        )
        return when (resId) {
            0 -> name
            else -> a.getString(resId)
        }
    }

    companion object {
        private const val RETRY_DISCOVERY_SECONDS = 30L

        private val ADD_PORT_DESCRIPTION = "PeerCast(${Build.BRAND} ${Build.DEVICE})"
        private val ADD_PORT_DURATION = 48.hours.inWholeSeconds.toInt()

        val REQ_TYPE_WIFI_ETHERNET = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .build()!!
    }

}