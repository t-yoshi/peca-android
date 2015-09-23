package org.peercast.pecaport;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.Nullable;
import android.util.Log;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.Transformer;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * (c) 2015, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
class NetworkDeviceManager {
    private static final String TAG = "NetworkingDevices";

    private final WifiManager mWifiManager;

    private NetworkDeviceManager(Context c) {
        mWifiManager = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
    }

    public static NetworkDeviceManager from(Context c) {
        return new NetworkDeviceManager(c);
    }
    /**
     * イーサネットアダプタ(eth0, eth1..)の情報を返す。
     */
    public List<NetworkInterfaceInfo.Ethernet> getEthernetInterface() {
        List<NetworkInterfaceInfo.Ethernet> infos = new ArrayList<>();
        try {

            for (NetworkInterface ni : Collections.list(
                    NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isLoopback() && !ni.isVirtual() && ni.getName()
                        .matches("^eth\\d+$") && //
                        ni.getInetAddresses()
                                .hasMoreElements())
                    infos.add(new NetworkInterfaceInfo.Ethernet(ni));
            }
        } catch (SocketException e) {
            Log.w(TAG, "getEthernetInterface()", e);
        }
        return infos;
    }

    public Collection<NetworkInterfaceInfo.Wifi> getWifiInterfaces() {
        final WifiInfo activeWifiInfo = mWifiManager.getConnectionInfo();
        if (activeWifiInfo == null)
            return Collections.emptyList();

        //Log.d(TAG, "getConfiguredNetworks: "+mWifiManager.getConfiguredNetworks());
        return CollectionUtils.collect(
                mWifiManager.getConfiguredNetworks(),
                new Transformer<WifiConfiguration, NetworkInterfaceInfo.Wifi>() {
                    @Override
                    public NetworkInterfaceInfo.Wifi transform(WifiConfiguration config) {
                        return new NetworkInterfaceInfo.Wifi(activeWifiInfo, config);
                    }
                });
    }

    public Collection<NetworkInterfaceInfo> getAllInterfaces() {
        return CollectionUtils.union(getEthernetInterface(), getWifiInterfaces());
    }

    @Nullable
    public NetworkInterfaceInfo getActiveInterface() {
        return CollectionUtils.find(getAllInterfaces(), new Predicate<NetworkInterfaceInfo>() {
            @Override
            public boolean evaluate(NetworkInterfaceInfo iface) {
                return iface.isActive();
            }
        });
    }


}
