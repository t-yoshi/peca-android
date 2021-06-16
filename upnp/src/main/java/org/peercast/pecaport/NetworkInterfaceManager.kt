package org.peercast.pecaport

import android.app.Application
import android.content.Context
import android.net.wifi.WifiManager
import org.fourthline.cling.android.AndroidUpnpServiceConfiguration
import java.net.Inet4Address

/**
 * (c) 2015, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */

class NetworkInterfaceManager(private val a: Application) {

    private val wifiManager =
            a.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    /**
     * Wifi以外のイーサネットアダプタ(eth0, eth1..)を返す。
     */
    val ethernetInterfaces: List<NetworkInterfaceInfo.Ethernet>
        get() = AndroidUpnpServiceConfiguration()
                .createNetworkAddressFactory()
                .networkInterfaces
                .asSequence()
                .filter { ni ->
                    ni.isUp && ni.inetAddresses.asSequence().any { it is Inet4Address } &&
                            (wifiInterface.isEmpty() ||
                                    wifiInterface[0].ipAddress !in ni.inetAddresses.asSequence().map { it.hostAddress })
                }
                .map { ni ->
                    NetworkInterfaceInfo.Ethernet(
                            a.getString(R.string.t_fmt_ethernet, ni.name),
                            ni.inetAddresses.asSequence().first { it is Inet4Address } as Inet4Address)
                }
                .toList()

    /**
     * アクティブなWifiを返す。
     */
    val wifiInterface: List<NetworkInterfaceInfo.Wifi>
        get() {
            val active = wifiManager.connectionInfo
            if (active == null || active.networkId == -1)
                return emptyList()
            return NetworkInterfaceInfo.Wifi(
                    a.getString(R.string.t_fmt_wifi, active.ssid),
                    active.ipAddress).let { listOf(it) }
        }

    val allInterfaces: List<NetworkInterfaceInfo>
        get() = wifiInterface + ethernetInterfaces


    fun findByName(name: String) = allInterfaces.firstOrNull { it.name == name }
}
