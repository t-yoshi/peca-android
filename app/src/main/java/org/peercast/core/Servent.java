package org.peercast.core;
/**
 * (c) 2013, T Yoshizawa
 *
 * Dual licensed under the MIT or GPL licenses.
 */

import android.os.Bundle;

public class Servent {

    private final Bundle mBundle;

    /**
     * @see Channel#getServents()
     */
    Servent(Bundle b) {
        if (b == null)
            throw new NullPointerException();
        mBundle = b;
    }

    public int getServent_ID() {
        return mBundle.getInt("servent_id");
    }

    public boolean isRelay() {
        return mBundle.getBoolean("relay");
    }

    public boolean isFirewalled() {
        return mBundle.getBoolean("firewalled");
    }

    public boolean isSetInfoFlg() {
        return mBundle.getBoolean("infoFlg");
    }


    public int getNumRelays() {
        return mBundle.getInt("numRelays");
    }

    public String getHost() {
        return mBundle.getString("host");
    }

    public int getPort() {
        return mBundle.getInt("port");
    }

    public int getTotalListeners() {
        return mBundle.getInt("totalListeners");
    }

    public int getTotalRelays() {
        return mBundle.getInt("totalRelays");
    }

    @Override
    public String toString() {
        String s = getClass().getSimpleName() + ": [";
        for (String k : mBundle.keySet()) {
            s += k + "=" + mBundle.get(k) + ", ";
        }
        return s + "]";
    }

    public String getVersion() {
        return mBundle.getString("version");
    }


}
