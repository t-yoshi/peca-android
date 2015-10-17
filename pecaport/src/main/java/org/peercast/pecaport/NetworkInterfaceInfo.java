package org.peercast.pecaport;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;

/**
 * @author (c) 2015, T Yoshizawa
 * Dual licensed under the MIT or GPL licenses.
 */
public abstract class NetworkInterfaceInfo {
    private static final String TAG = "NetworkInterfaceInfo";
    private final String mName;
    protected boolean mIsActive;
    protected Inet4Address mPrivateAddress;
    protected String mHardwareAddress;


    protected NetworkInterfaceInfo(String name) {
        mName = name;
    }

    /**このNICの名前を返す。
     * @return ex. "eth0" or "\"SSDIDxxxx\""
     * */
    public String getName() {
        return mName;
    }

    public String getDisplayName(Context c) {
        return c.getString(
                this instanceof Wifi ? R.string.t_fmt_wifi : R.string.t_fmt_ethernet,
                getName()
        );
    }

    /**
     * 接続状態にあるか
     */
    public boolean isActive() {
        return mIsActive;
    }

    /**
     * このNICのハードウェアアドレスを返す。
     *
     * @deprecated Android6よりWifiInfo#getMacAddress()は"02:00:00:00:00:00"を返す
     * @return ex. "12:34:56:78:90:ab"
     * @throws IllegalStateException アクティブでないとき
     */
    @NonNull
    public String getHardwareAddress() {
        checkActive();
        return mHardwareAddress;
    }

    /**
     * このNICのPrivate IPを返す。
     * @return  ex. "192.168.0.6"
     * @throws IllegalStateException アクティブでないとき
     */
     public Inet4Address getPrivateAddress(){
        checkActive();
        return mPrivateAddress;
    }

    private void checkActive() {
        if (!isActive())
            throw new IllegalStateException("not active");
    }

    abstract protected String getType();

    @Override
    public String toString() {
        return String.format("%s [name=%s, ip=%s, mac=%s, active=%s]", getType(),
                getName(), mPrivateAddress, mHardwareAddress, mIsActive);
    }

    public static class Ethernet extends NetworkInterfaceInfo {

        Ethernet(NetworkInterface n) throws SocketException {
            super(n.getName());
            mIsActive = n.isUp();

            byte[] addr = n.getHardwareAddress();
            String[] buf = new String[addr.length];
            for (int i = 0; i < addr.length; i++)
                buf[i] = String.format("%02X", addr[i]);
            mHardwareAddress = TextUtils.join(":", buf);

            for (InetAddress ip : Collections.list(n.getInetAddresses())) {
                if (ip instanceof Inet4Address) {
                    mPrivateAddress = (Inet4Address) ip;
                    return;
                }
            }
            throw new SocketException("Missing IPv4 address");
        }

        @Override
        protected String getType() {
            return "Ethernet";
        }
    }

    public static class Wifi extends NetworkInterfaceInfo {

        Wifi(WifiInfo activeWifi, WifiConfiguration config)  {
            super(config.SSID);
            //Log.d(TAG, "WifiInfo: "+ info.getSSID() + " id=" + info.getNetworkId());
            //Log.d(TAG, "WifiConfiguration: " + c.SSID + ", id=" + c.networkId);

            mIsActive = activeWifi.getNetworkId() != -1 &&
                    activeWifi.getNetworkId() == config.networkId;
            if (mIsActive) {
                mHardwareAddress  = activeWifi.getMacAddress();
                int ip4 = activeWifi.getIpAddress();
                mPrivateAddress = ipToInetAddress(ip4);
            }
        }

        @Override
        protected String getType() {
            return "Wifi";
        }

        private static Inet4Address ipToInetAddress(int ip) {
            try {
                return (Inet4Address) InetAddress.getByAddress(
                        new byte[]{
                                (byte) (ip >> 0 & 0xff),
                                (byte) (ip >> 8 & 0xff),
                                (byte) (ip >> 16 & 0xff),
                                (byte) (ip >> 24 & 0xff)
                        }
                );
            } catch (UnknownHostException e){
                throw new RuntimeException(e);
            }
        }


    }

}
