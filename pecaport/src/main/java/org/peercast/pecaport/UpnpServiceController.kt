package org.peercast.pecaport

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.fourthline.cling.android.AndroidUpnpService
import org.fourthline.cling.android.AndroidUpnpServiceImpl
import org.fourthline.cling.model.message.header.RootDeviceHeader
import org.fourthline.cling.model.meta.RemoteDevice
import org.fourthline.cling.model.meta.RemoteService
import org.fourthline.cling.model.types.UDADeviceType
import org.fourthline.cling.model.types.UDAServiceType
import org.fourthline.cling.registry.DefaultRegistryListener
import org.fourthline.cling.registry.Registry
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.slf4j.LoggerFactory
import timber.log.Timber
import kotlin.coroutines.resume

/**
 * Androidサービスにバインドされたと同時にInternetGatewayDeviceを検索する。<br></br>
 *
 * @author (c) 2015, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
class UpnpServiceController : KoinComponent {
    private val a by inject<Application>()

    data class DiscoveredResult(
            val registry: Registry,
            /** ルーター*/
            val device: RemoteDevice,

            /**WANPPPConnectionまたはWANIPConnection*/
            val wanConnections: List<RemoteService>
    )

    private val logger = LoggerFactory.getLogger(javaClass.name)
    private var upnpService: AndroidUpnpService? = null

    private val discoveredLiveData_ = MutableLiveData<DiscoveredResult?>()
    val discoveredLiveData: LiveData<DiscoveredResult?> = discoveredLiveData_


    private val registryListener = object : DefaultRegistryListener() {
        override fun remoteDeviceDiscoveryStarted(registry: Registry?, device: RemoteDevice?) {
            logger.debug("remoteDeviceDiscoveryStarted: $registry,$device")
        }

        override fun remoteDeviceAdded(registry: Registry, device: RemoteDevice) {
            logger.debug("remoteDeviceAdded: $registry, $device")

            if (device.isRoot && device.type == TYPE_INTERNET_GATEWAY_DEVICE) {
                var services = device.findServices(TYPE_WAN_PPP_CONNECTION)
                if (services.isEmpty())
                    services = device.findServices(TYPE_WAN_IP_CONNECTION)
                discoveredLiveData_.postValue(DiscoveredResult(registry, device, services.toList()))
            }
        }

        override fun remoteDeviceDiscoveryFailed(registry: Registry, device: RemoteDevice?, ex: Exception?) {
            logger.error("remoteDeviceDiscoveryFailed: $registry, $device")
            discoveredLiveData_.postValue(null)
        }
    }

    val isConnected: Boolean
        get() = upnpService != null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            upnpService = service as AndroidUpnpService
            Timber.d("onServiceConnected: $service")

            service.registry.let {
                it.removeAllRemoteDevices()
                it.addListener(registryListener)
            }
            service.controlPoint.search(RootDeviceHeader(), 3)
        }

        /**
         * サービスがOSにKillされた場合にだけ呼ばれる。
         * */
        override fun onServiceDisconnected(unused: ComponentName?) {
            Timber.d("onServiceDisconnected()")
            if (upnpService == null)
                return
            discoveredLiveData_.postValue(null)
            upnpService?.registry?.removeListener(registryListener)
            upnpService = null
        }
    }

    fun bindService() {
        if (isConnected)
            return
        a.bindService(
                Intent(a, AndroidUpnpServiceImpl::class.java),
                serviceConnection, Context.BIND_AUTO_CREATE
        )
    }

    fun unbindService() {
        a.unbindService(serviceConnection)
        serviceConnection.onServiceDisconnected(null)
    }

    companion object {
        private val TYPE_INTERNET_GATEWAY_DEVICE = UDADeviceType("InternetGatewayDevice")
        private val TYPE_WAN_PPP_CONNECTION = UDAServiceType("WANPPPConnection")
        private val TYPE_WAN_IP_CONNECTION = UDAServiceType("WANIPConnection")
    }

}

@MainThread
suspend fun LiveData<UpnpServiceController.DiscoveredResult?>.await()
        : UpnpServiceController.DiscoveredResult? = suspendCancellableCoroutine { co ->
    val observer = Observer<UpnpServiceController.DiscoveredResult?> {
        if (!co.isCompleted)
            co.resume(it)
    }
    observeForever(observer)
    co.invokeOnCancellation {
        GlobalScope.launch(Dispatchers.Main) {
            removeObserver(observer)
        }
    }
}