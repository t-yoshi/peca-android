package org.peercast.pecaport

import java.net.Inet4Address


/**
 * (c) 2015, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */

sealed class NetworkInterfaceInfo(
        /**
         * このNICの名前を返す。
         */
        val name: String,
        /**
         * このNICのPrivate IPを返す。
         * @return  ex. "192.168.0.6"
         */
        val ipAddress: String
) {

    override fun toString(): String {
        return "${javaClass.simpleName} [name=$name, ipAddress=$ipAddress]"
    }

    class Ethernet(name: String, addr: Inet4Address) : NetworkInterfaceInfo(name, addr.hostAddress)

    class Wifi(name: String, ip4: Int) : NetworkInterfaceInfo(name, ipToString(ip4)) {
        companion object {
            private fun ipToString(ip: Int): String {
                return "%d.%d.%d.%d".format(
                        (ip shr 0 and 0xff),
                        (ip shr 8 and 0xff),
                        (ip shr 16 and 0xff),
                        (ip shr 24 and 0xff)
                )
            }
        }
    }
}
